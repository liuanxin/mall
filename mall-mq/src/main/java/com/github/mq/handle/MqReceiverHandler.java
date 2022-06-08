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
import java.util.function.Consumer;

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
     * @param consumer 业务处理时异常会自动发送 nack, 无异常 或 异常次数超过指定数量 时会发送 ack
     */
    public void doConsume(Message message, Channel channel, Consumer<String> consumer) {
        long start = System.currentTimeMillis();
        String desc = "";
        try {
            // msgId 放在 messageId, traceId 放在 correlationId
            MessageProperties messageProperties = message.getMessageProperties();
            LogUtil.bindBasicInfo(messageProperties.getCorrelationId());
            long deliveryTag = messageProperties.getDeliveryTag();

            MqData selfData = JsonUtil.toObject(new String(message.getBody()), MqData.class);
            if (U.isNull(selfData)) {
                ack(channel, deliveryTag, "消费数据为空, 发送 ack 时异常");
                return;
            }

            MqInfo mqInfo = MqInfo.from(selfData.getMqInfo());
            if (U.isNull(mqInfo)) {
                ack(channel, deliveryTag, "消费数据无类型, 发送 ack 时异常");
                return;
            }

            desc = mqInfo.showDesc();
            String json = selfData.getJson();
            if (U.isBlank(json)) {
                ack(channel, deliveryTag, String.format("消费 %s 数据 json 为空, 发送 ack 时异常", desc));
                return;
            }

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始消费 mq({}), 消息发送时间({})", desc, DateUtil.formatDateTimeMs(selfData.getSendTime()));
            }
            // msgId 放在 messageId, traceId 放在 correlationId
            handleData(json, messageProperties.getMessageId(), mqInfo, desc, deliveryTag, channel, consumer);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 mq{} 结束, 耗时: ({})", (U.isBlank(desc) ? "" : String.format("(%s)", desc)),
                        DateUtil.toHuman(System.currentTimeMillis() - start));
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
     * @param consumer 业务处理时异常会自动发送 nack, 无异常 或 异常次数超过指定数量 时会发送 ack
     */
    public void doConsumeJustJson(MqInfo mqInfo, Message message, Channel channel, Consumer<String> consumer) {
        long start = System.currentTimeMillis();
        String desc = mqInfo.getDesc();
        try {
            MessageProperties messageProperties = message.getMessageProperties();
            // msgId 放在 messageId, traceId 放在 correlationId
            LogUtil.bindBasicInfo(messageProperties.getCorrelationId());

            long deliveryTag = messageProperties.getDeliveryTag();
            String json = new String(message.getBody());
            if (U.isBlank(json)) {
                ack(channel, deliveryTag, String.format("消费 %s 数据为空, 发送 ack 时异常", desc));
                return;
            }

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始消费 mq({})", desc);
            }
            // msgId 放在 messageId, traceId 放在 correlationId
            handleData(json, messageProperties.getMessageId(), mqInfo, desc, deliveryTag, channel, consumer);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 mq{} 结束, 耗时: ({})", desc, DateUtil.toHuman(System.currentTimeMillis() - start));
            }
            LogUtil.unbind();
        }
    }

    /** 在每一个节点都要确保会发送 ack 或 nack */
    private void handleData(String json, String messageId, MqInfo mqInfo, String desc,
                            long deliveryTag, Channel channel, Consumer<String> consumer) {
        String msgId = getMsgId(messageId, json);
        if (U.isBlank(msgId)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("{}消费数据({})时没有消息 id", desc, json);
            }
            ack(channel, deliveryTag, String.format("消费(%s)数据(%s)时没有消息 id, 发送 ack 时异常", desc, json));
            return;
        }
        if (redissonService.tryLock(msgId)) {
            try {
                doDataConsume(json, msgId, mqInfo.name().toLowerCase(), desc, deliveryTag, channel, consumer);
            } finally {
                redissonService.unlock(msgId);
            }
            return;
        }
        ack(channel, deliveryTag, String.format("消息(%s)正在被处理, 无需重复消费, 当前消息发送 ack 时异常", msgId));
    }

    private void doDataConsume(String json, String msgId, String businessType, String desc,
                               long deliveryTag, Channel channel, Consumer<String> consumer) {
        MqReceive model = mqReceiveService.queryByMsg(msgId);
        boolean needAdd = U.isNull(model);
        if (needAdd) {
            model = new MqReceive();
            model.setMsgId(msgId);
            model.setBusinessType(businessType);
            model.setRetryCount(0);
            model.setMsg(json);
        }

        // 成功了就只写一次消费成功, 失败了也只写一次, 上面不写初始, 少操作一次 db
        boolean ack = true;
        String ackMsg = "";
        int currentRetryCount = U.toInt(model.getRetryCount());
        try {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("{}消费数据({})", desc, json);
            }
            consumer.accept(json);
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("{}消费成功", desc);
            }

            model.setStatus(2);
            ackMsg = desc + "消费成功";
        } catch (Exception e) {
            String failMsg = e.getMessage();
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{}消费失败", desc, e);
            }
            model.setStatus(1);
            // 如果重试次数达到设定的值则发送 ack, 否则发送 nack
            if (currentRetryCount > consumerRetryCount) {
                ackMsg = String.format("消费(%s)失败且重试(%s)达到上限(%s)", desc, currentRetryCount, consumerRetryCount);
            } else {
                ack = false;
                ackMsg = desc + "消费失败";
            }
        } finally {
            model.setRemark(ackMsg);
            if (needAdd) {
                mqReceiveService.add(model);
            } else {
                model.setRetryCount(currentRetryCount + 1);
                mqReceiveService.updateById(model);
            }

            if (ack) {
                ack(channel, deliveryTag, ackMsg + ", 发送 ack 时异常");
            } else {
                nack(channel, deliveryTag, ackMsg + ", 发送 nack 时异常");
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

    private void ack(Channel channel, long deliveryTag, String errorDesc) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(errorDesc, e);
            }
        }
    }

    private void nack(Channel channel, long deliveryTag, String errorDesc) {
        try {
            channel.basicNack(deliveryTag, false, true);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(errorDesc, e);
            }
        }
    }
}
