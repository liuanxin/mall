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
     * 消息处理. !!!消费体一定要包含 msgId 信息!!!
     *
     * @param fun 业务处理: 入参是数据对应的 json, 返回 searchKey
     */
    public void doConsume(MqInfo mqInfo, Message message, Function<String, String> fun) {
        if (U.isNull(mqInfo)) {
            return;
        }
        long start = System.currentTimeMillis();
        String desc = mqInfo.getDesc();
        try {
            MessageProperties messageProperties = message.getMessageProperties();
            // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
            LogUtil.bindBasicInfo(messageProperties.getCorrelationId());

            String json = new String(message.getBody());
            if (U.isBlank(json)) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("消费 {} 数据({})是空的", desc, json);
                }
                return;
            }

            // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
            String msgId = getMsgId(messageProperties.getMessageId(), json);
            if (U.isBlank(msgId)) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("消费 {} 数据({})没有消息 id", desc, json);
                }
                return;
            }

            handleData(msgId, json, mqInfo, desc, fun);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 结束, 耗时: ({})", desc, DateUtil.toHuman(System.currentTimeMillis() - start));
            }
            LogUtil.unbind();
        }
    }

    /** 在每一个节点都要确保会发送 ack 或 nack */
    private void handleData(String msgId, String json, MqInfo mqInfo, String desc, Function<String, String> fun) {
        if (redissonService.tryLock(msgId)) {
            try {
                doDataConsume(msgId, json, mqInfo.name().toLowerCase(), desc, fun);
            } finally {
                redissonService.unlock(msgId);
            }
        } else {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({}), 正在处理", desc, msgId);
            }
        }
    }

    private void doDataConsume(String msgId, String json, String businessType, String desc, Function<String, String> fun) {
        MqReceive model = null;
        boolean needAdd = false;
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

            String oldRemark = model.getRemark();
            remark = (U.isBlank(oldRemark) ? "" : (oldRemark + ";;")) + desc + "消费成功";
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费 {} 数据({})失败", desc, msgId, e);
            }
            status = 1;
            String oldRemark = U.isNull(model) ? null : model.getRemark();
            String appendRemark = U.isBlank(oldRemark) ? "" : (oldRemark + ";;");
            if (currentRetryCount < consumerRetryCount) {
                remark = appendRemark + String.format("消费 %s 数据(%s)失败(%s)", desc, msgId, e.getMessage());
            } else {
                remark = appendRemark + String.format("消费 %s 数据(%s)失败(%s)且重试(%s)达到上限(%s)",
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
        }
    }

    @SuppressWarnings("unchecked")
    private String getMsgId(String msgId, String json) {
        if (U.isNotBlank(msgId)) {
            return msgId;
        }

        MqData mqData = JsonUtil.toObject(json, MqData.class);
        String dataJson = U.isNull(mqData) ? json : mqData.getJson();
        // 从消息体里面获取 msgId
        Map<String, Object> map = JsonUtil.toObject(dataJson, Map.class);
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
}
