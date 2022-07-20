package com.github.mq.handle;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqConst;
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

/***
 * # ack 模式设置为自动(异常将会 nack, 否则将会 ack)
 * spring.rabbitmq.listener.simple.acknowledge-mode = auto
 * # 开启重试
 * spring.rabbitmq.listener.simple.retry.enabled = true
 * # 第一次重试的时间间隔, 默认是 1 秒
 * spring.rabbitmq.listener.simple.retry.initial-interval = 5s
 * # 最大重试次数, 默认是 3 次
 * spring.rabbitmq.listener.simple.retry.max-attempts = 5
 * # 两次重试的间隔, 默认是 10 秒
 * spring.rabbitmq.listener.simple.retry.max-interval = 5s
 *
 * @see org.springframework.boot.autoconfigure.amqp.RabbitProperties
 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass(RabbitListener.class)
public class MqReceiverHandler {

    @Value("${mq.consumerRetryCount:3}")
    private int consumerRetryCount;

    @Value("${mq.consumerMsgIdKey:msgId,messageId,msg_id,message_id}")
    private String msgIdKey;

    private final MqReceiveService mqReceiveService;
    private final RedissonService redissonService;

    /**
     * 消息处理. !!!消费体一定要包含 msgId 信息!!!
     *
     * @param fun 业务处理: 入参是数据对应的 json, 返回 searchKey
     */
    public void doConsume(MqInfo mqInfo, Message message, Function<String, String> fun) {
        if (U.isNull(mqInfo)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("没有队列信息, 无法处理");
            }
            return;
        }
        String json = new String(message.getBody());
        if (U.isBlank(json)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费数据({})是空的, 无需处理", json);
            }
            return;
        }
        MqData mqData = JsonUtil.toObjectNil(json, MqData.class);
        if (U.isNotNull(mqData)) {
            MqInfo info = MqInfo.from(mqData.getMqInfo());
            if (U.isNotNull(info) && info != mqInfo) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("消费数据({})信息是({})却在用({})消费, 请检查代码", json, info, mqInfo);
                }
                return;
            }
        }

        String desc = mqInfo.showDesc();
        MessageProperties mp = message.getMessageProperties();
        // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
        String msgId = getMsgId(mp.getMessageId(), mqData, json);
        // msgId 如果没有不处理
        if (U.isBlank(msgId)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({})没有 msgId, 请与发送方沟通", desc, json);
            }
            return;
        }

        long start = System.currentTimeMillis();
        try {
            // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
            LogUtil.putTraceId(U.defaultIfBlank(mp.getCorrelationId(), (U.isNull(mqData) ? "" : mqData.getTraceId())));
            handleData(msgId, mqData, json, mqInfo, desc, fun);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                long now = System.currentTimeMillis();
                LogUtil.ROOT_LOG.info("消费 {} 结束, 耗时: ({})", desc, DateUtil.toHuman(now - start));
            }
            LogUtil.unbind();
        }
    }

    /** 在每一个节点都要确保会发送 ack 或 nack */
    private void handleData(String msgId, MqData mqData, String json, MqInfo mqInfo, String desc, Function<String, String> fun) {
        if (redissonService.tryLock(msgId)) {
            try {
                doDataConsume(msgId, mqData, json, mqInfo, desc, fun);
            } finally {
                redissonService.unlock(msgId);
            }
        } else {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({}), 正在处理", desc, msgId);
            }
        }
    }

    private void doDataConsume(String msgId, MqData mqData, String json, MqInfo mqInfo, String desc, Function<String, String> fun) {
        MqReceive model = null;
        boolean needAdd = false;
        String remark = U.EMPTY;
        int status = MqConst.INIT;
        int currentRetryCount = 0;
        try {
            model = mqReceiveService.queryByMsg(msgId);
            needAdd = U.isNull(model);
            if (needAdd) {
                model = new MqReceive();
                model.setMsgId(msgId);
                model.setType(mqInfo.name().toLowerCase());
                model.setRetryCount(0);
                model.setMsg(json);
            }
            currentRetryCount = U.toInt(model.getRetryCount());

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("开始消费 {} 数据({})", desc, json);
            }
            String searchKey = fun.apply(U.isNull(mqData) ? json : U.defaultIfBlank(mqData.getJson(), json));
            if (U.isNotBlank(model.getSearchKey()) && U.isNotBlank(searchKey)) {
                model.setSearchKey(U.toStr(searchKey));
            }
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费({})数据({})成功", desc, msgId);
            }
            status = MqConst.SUCCESS;
            remark = String.format("<%s : 消费(%s)数据成功>%s", DateUtil.nowDateTime(), desc, U.toStr(model.getRemark()));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费({})数据({})异常", desc, msgId, e);
            }
            status = MqConst.FAIL;
            String oldRemark = U.toStr(U.isNull(model) ? null : model.getRemark());
            String now = DateUtil.nowDateTime();
            if (currentRetryCount < consumerRetryCount) {
                remark = String.format("<%s : 消费(%s)数据异常(%s)>%s", now, desc, e.getMessage(), oldRemark);
                throw e;
            } else {
                remark = String.format("<%s : 消费(%s)数据异常(%s)且重试(%s)达到上限(%s)>%s", now, desc, e.getMessage(),
                        currentRetryCount, consumerRetryCount, oldRemark);
            }
        } finally {
            if (U.isNotNull(model)) {
                if (needAdd) {
                    model.setStatus(status);
                    model.setRemark(remark);
                    mqReceiveService.add(model);
                } else {
                    MqReceive update = new MqReceive();
                    update.setId(model.getId());
                    update.setStatus(status);
                    update.setRemark(remark);
                    update.setRetryCount(currentRetryCount + 1);
                    mqReceiveService.updateById(update);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getMsgId(String defaultId, MqData mqData, String json) {
        if (U.isNotBlank(defaultId)) {
            return defaultId;
        }

        String dataJson = U.isNull(mqData) ? json : U.defaultIfBlank(mqData.getJson(), json);
        Map<String, Object> map = JsonUtil.toObjectNil(dataJson, Map.class);
        if (A.isNotEmpty(map)) {
            for (String key : msgIdKey.split(",")) {
                String msgId = U.toStr(map.get(key.trim()));
                if (U.isNotBlank(msgId)) {
                    return msgId;
                }
            }
        }
        return U.EMPTY;
    }
}
