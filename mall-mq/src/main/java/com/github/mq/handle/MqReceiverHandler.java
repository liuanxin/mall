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
            if (U.isNotNull(info)) {
                if (U.isNull(mqInfo)) {
                    mqInfo = info;
                } else if (info != mqInfo) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("消费数据({})信息是({})却在用({})消费, 请检查代码", json, info, mqInfo);
                    }
                    return;
                }
            }
        }
        if (U.isNull(mqInfo)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("没有队列信息, 无法处理");
            }
            return;
        }

        String mqDesc = mqInfo.getDesc();
        MessageProperties mp = message.getMessageProperties();
        // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
        String msgId = getMsgId(mp.getMessageId(), mqData, json);
        // msgId 如果没有不处理
        if (U.isBlank(msgId)) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({})没有 msgId, 请与发送方沟通", mqDesc, json);
            }
            return;
        }

        long start = System.currentTimeMillis();
        try {
            // 发布消息时: msgId 放在 messageId, traceId 放在 correlationId
            LogUtil.bindBasicInfo(U.defaultIfBlank(mp.getCorrelationId(), (U.isNull(mqData) ? "" : mqData.getTraceId())));
            handleData(msgId, mqData, json, mqInfo, mqDesc, fun);
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                long now = System.currentTimeMillis();
                LogUtil.ROOT_LOG.info("消费 {} 结束, 耗时: ({})", mqDesc, DateUtil.toHuman(now - start));
            }
            LogUtil.unbind();
        }
    }

    /** 在每一个节点都要确保会发送 ack 或 nack */
    private void handleData(String msgId, MqData mqData, String json, MqInfo mqInfo,
                            String desc, Function<String, String> fun) {
        if (redissonService.tryLock(msgId)) {
            try {
                doDataConsume(msgId, mqData, json, mqInfo.name().toLowerCase(), desc, fun);
            } finally {
                redissonService.unlock(msgId);
            }
        } else {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({}), 正在处理", desc, msgId);
            }
        }
    }

    private void doDataConsume(String msgId, MqData mqData, String json, String businessType,
                               String desc, Function<String, String> fun) {
        // 0.初始, 1.失败, 2.成功(需要重试则改为 1)
        int fail = 1, success = 2;

        MqReceive model = null;
        boolean needAdd = false;
        String remark = "";
        int status = success;
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
            String dataJson = U.isNull(mqData) ? json : U.defaultIfBlank(mqData.getJson(), json);
            String searchKey = fun.apply(dataJson);
            model.setSearchKey(U.toStr(searchKey));
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("消费 {} 数据({})成功", desc, msgId);
            }

            String oldRemark = model.getRemark();
            remark = (U.isBlank(oldRemark) ? "" : (oldRemark + ";;")) + desc + "消费成功";
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("消费 {} 数据({})失败", desc, msgId, e);
            }
            status = fail;
            String oldRemark = U.isNull(model) ? null : model.getRemark();
            String appendRemark = U.isBlank(oldRemark) ? "" : (oldRemark + ";;");
            if (currentRetryCount < consumerRetryCount) {
                remark = appendRemark + String.format("消费 %s 数据(%s)失败(%s)", desc, msgId, e.getMessage());
            } else {
                remark = appendRemark + String.format("消费 %s 数据(%s)失败(%s)且重试(%s)达到上限(%s)",
                        desc, msgId, e.getMessage(), currentRetryCount, consumerRetryCount);
            }
            throw e;
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
    private String getMsgId(String defaultId, MqData mqData, String json) {
        if (U.isNotBlank(defaultId)) {
            return defaultId;
        }

        String dataJson = U.isNull(mqData) ? json : U.defaultIfBlank(mqData.getJson(), json);
        Map<String, Object> map = JsonUtil.toObjectNil(dataJson, Map.class);
        if (A.isNotEmpty(map)) {
            for (String key : msgIdKey.split(",")) {
                String trace = U.toStr(map.get(key.trim()));
                if (U.isNotBlank(trace)) {
                    return trace;
                }
            }
        }
        return U.EMPTY;
    }
}
