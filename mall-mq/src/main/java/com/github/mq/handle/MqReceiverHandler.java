package com.github.mq.handle;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqData;
import com.github.mq.constant.MqInfo;
import com.github.mq.model.MqReceive;
import com.github.mq.service.MqReceiveService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
@Configuration
@ConditionalOnClass(RabbitListener.class)
public class MqReceiverHandler {

    @Value("${mq.consumerRetryCount:3}")
    private int consumerRetryCount;

    private final MqReceiveService mqReceiveService;
    private final RedissonService redissonService;

    /**
     * 消息处理, 需要设置 spring.rabbitmq.listener.simple.acknowledge-mode = manual 才可以手动处理 ack
     * <p>
     * 消费成功时: 发送 ack 并写记录(状态为成功)
     * 消费失败时:
     *   未达到上限则 nack(重回队列) 并写记录(状态为失败)
     *   已达到上限则 ack 并写记录(状态为失败)
     * <p>
     * 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
     *
     * @param fun 业务处理时异常会自动发送 nack, 无异常 或 异常次数超过指定数量 时会发送 ack, 入参是数据对应的 json, 返回 searchKey
     */
    public void doConsume(MqInfo mqInfo, Message message, Channel channel, Function<String, String> fun) {
        long start = System.currentTimeMillis();
        String desc = mqInfo.showDesc();
        try {
            // msgId 放在 messageId, traceId 放在 correlationId
            MessageProperties messageProperties = message.getMessageProperties();
            LogUtil.bindBasicInfo(messageProperties.getCorrelationId());
            long deliveryTag = messageProperties.getDeliveryTag();

            MqData selfData = JsonUtil.toObject(new String(message.getBody()), MqData.class);
            if (U.isNull(selfData)) {
                ack(desc, "", channel, deliveryTag, "消费数据为空.");
                return;
            }

            String json = selfData.getJson();
            if (U.isBlank(json)) {
                ack(desc, "", channel, deliveryTag, String.format("消费 %s 数据 json 为空.", desc));
                return;
            }

            String msgId = getMsgId(messageProperties.getMessageId(), json);
            if (U.isBlank(msgId)) {
                ack(desc, msgId, channel, deliveryTag, String.format("消费(%s)数据(%s)时没有消息 id.", desc, json));
                return;
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始消费 mq({} : {}), 消息发送时间({})", desc, msgId, DateUtil.formatDateTimeMs(selfData.getSendTime()));
            }
            // msgId 放在 messageId, traceId 放在 correlationId
            handleData(json, msgId, mqInfo, desc, deliveryTag, channel, fun);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 结束, 耗时: ({})", desc, DateUtil.toHuman(System.currentTimeMillis() - start));
            }
            LogUtil.unbind();
        }
    }

    /**
     * 消息处理(仅 json 消息), 需要设置 spring.rabbitmq.listener.simple.acknowledge-mode = manual 才可以手动处理 ack
     * <p>
     * 消费成功时: 发送 ack 并写记录(状态为成功)
     * 消费失败时:
     *   未达到上限则 nack(重回队列) 并写记录(状态为失败)
     *   已达到上限则 ack 并写记录(状态为失败)
     * <p>
     * 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
     *
     * @param fun 业务处理时异常会自动发送 nack, 无异常 或 异常次数超过指定数量 时会发送 ack, 入参是数据对应的 json, 返回 searchKey
     */
    public void doConsumeJustJson(MqInfo mqInfo, Message message, Channel channel, Function<String, String> fun) {
        long start = System.currentTimeMillis();
        String desc = mqInfo.getDesc();
        try {
            MessageProperties messageProperties = message.getMessageProperties();
            // msgId 放在 messageId, traceId 放在 correlationId
            LogUtil.bindBasicInfo(messageProperties.getCorrelationId());

            long deliveryTag = messageProperties.getDeliveryTag();
            String json = new String(message.getBody());
            if (U.isBlank(json)) {
                ack(desc, "", channel, deliveryTag, String.format("消费 %s 时为空.", desc));
                return;
            }

            String msgId = getMsgId(messageProperties.getMessageId(), json);
            if (U.isBlank(msgId)) {
                ack(desc, msgId, channel, deliveryTag, String.format("消费 %s 数据(%s), 没有消息 id.", desc, json));
                return;
            }
            // msgId 放在 messageId, traceId 放在 correlationId
            handleData(json, msgId, mqInfo, desc, deliveryTag, channel, fun);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 结束, 耗时: ({})", desc, DateUtil.toHuman(System.currentTimeMillis() - start));
            }
            LogUtil.unbind();
        }
    }

    /** 在每一个节点都要确保会发送 ack 或 nack */
    private void handleData(String json, String msgId, MqInfo mqInfo, String desc,
                            long deliveryTag, Channel channel, Function<String, String> fun) {
        if (redissonService.tryLock(msgId)) {
            try {
                doDataConsume(json, msgId, mqInfo.name().toLowerCase(), desc, deliveryTag, channel, fun);
            } finally {
                redissonService.unlock(msgId);
            }
        } else {
            ack(desc, msgId, channel, deliveryTag, String.format("消费 %s 数据(%s), 数据正在被处理, 无需重复消费.", desc, msgId));
        }
    }

    private void doDataConsume(String json, String msgId, String businessType, String desc,
                               long deliveryTag, Channel channel, Function<String, String> fun) {
        MqReceive model = null;
        boolean needAdd = false;
        boolean ack = true;
        String remark = "";
        int status = 2;
        int currentRetryCount = 0;
        try {
            model = mqReceiveService.queryByMsg(msgId);
            needAdd = U.isNull(model);
            if (needAdd) {
                model = new MqReceive();
                model.setMsgId(msgId);
                model.setBusinessType(businessType);
                model.setRetryCount(0);
                model.setMsg(json);
            }
            currentRetryCount = U.toInt(model.getRetryCount());

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始消费 {} 数据({})", desc, json);
            }
            model.setSearchKey(U.toStr(fun.apply(json)));
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({})成功", desc, msgId);
            }

            remark = desc + "消费成功";
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费 {} 数据({})失败", desc, msgId, e);
            }
            status = 1;
            // 如果重试次数未达到设定的值则 nack 进行重试, 否则发送 ack
            if (currentRetryCount < consumerRetryCount) {
                ack = false;
                remark = String.format("消费 %s 数据(%s)失败(%s)", desc, msgId, e.getMessage());
            } else {
                remark = String.format("消费 %s 数据(%s)失败(%s)且重试(%s)达到上限(%s)",
                        desc, msgId, e.getMessage(), currentRetryCount, consumerRetryCount);
            }
        } finally {
            // 成功了就只写一次消费成功, 失败了也只写一次, 上面不写初始, 少操作一次 db
            if (U.isNotNull(model)) {
                model.setStatus(status);
                model.setRemark(remark);
                if (needAdd) {
                    mqReceiveService.add(model);
                } else {
                    model.setRetryCount(currentRetryCount + 1);
                    mqReceiveService.updateById(model);
                }
            }

            if (ack) {
                ack(desc, msgId, channel, deliveryTag, remark);
            } else {
                nack(desc, msgId, channel, deliveryTag, remark);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getMsgId(String msgId, String json) {
        if (U.isNotBlank(msgId)) {
            return msgId;
        }
        // 从消息体里面获取 msgId
        Map<String, Object> map = JsonUtil.toObject(json, Map.class);
        if (A.isEmpty(map)) {
            return null;
        }

        String messageId = U.toStr(map.get("msgId"));
        if (U.isNotBlank(messageId)) {
            return messageId;
        }
        messageId = U.toStr(map.get("messageId"));
        if (U.isNotBlank(messageId)) {
            return messageId;
        }
        messageId = U.toStr(map.get("message_id"));
        if (U.isNotBlank(messageId)) {
            return messageId;
        }
        return U.toStr(map.get("msg_id"));
    }

    private void ack(String desc, String msgId, Channel channel, long deliveryTag, String sendRemark) {
        try {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("消费 {} 数据({}), 发送 ack, 理由({})", desc, msgId, sendRemark);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费 {} 数据({}), 发送 ack, 理由({}), 异常", desc, msgId, sendRemark, e);
            }
        }
    }

    private void nack(String desc, String msgId, Channel channel, long deliveryTag, String sendRemark) {
        try {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({}), 发送 nack, 理由({})", desc, msgId, sendRemark);
            }
            channel.basicNack(deliveryTag, false, true);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费 {} 数据({}), 发送 nack, 理由({}), 异常", desc, msgId, sendRemark, e);
            }
        }
    }
}
