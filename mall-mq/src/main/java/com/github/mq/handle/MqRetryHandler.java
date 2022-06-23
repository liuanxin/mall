package com.github.mq.handle;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqInfo;
import com.github.mq.model.MqReceive;
import com.github.mq.model.MqSend;
import com.github.mq.service.MqReceiveService;
import com.github.mq.service.MqSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class MqRetryHandler {

    private static final String MQ_RECEIVE_DESC = "重试 mq 消费";
    private static final String MQ_SEND_DESC = "重试 mq 发送";

    @Value("mq.retryLimit:20")
    private int mqRetryLimit;

    @Value("mq.maxRetryCount:5")
    private int maxRetryCount;

    private final RedissonService redissonService;
    private final MqSendService mqSendService;
    private final MqReceiveService mqReceiveService;
    private final MqSenderHandler mqSenderHandler;


    public boolean handlerReceive(String desc) {
        for (;;) {
            List<MqReceive> mqReceiveList = mqReceiveService.queryRetryMsg(maxRetryCount, mqRetryLimit);
            if (A.isNotEmpty(mqReceiveList)) {
                return true;
            }
            for (MqReceive mqReceive : mqReceiveList) {
                retrySingle(desc, mqReceive);
            }
            if (mqReceiveList.size() < mqRetryLimit) {
                return true;
            }
        }
    }

    private void retrySingle(String desc, MqReceive mqReceive) {
        // noinspection DuplicatedCode
        MqInfo mqInfo = MqInfo.from(mqReceive.getBusinessType());
        if (U.isNull(mqInfo)) {
            // 没有 mq-info 的数据直接置为成功, 无法重试
            MqReceive update = new MqReceive();
            update.setId(mqReceive.getId());
            update.setStatus(2);
            update.setRemark(mqReceive.getRemark() + ";;没有这个 business_type 的场景");
            mqReceiveService.updateById(update);
            return;
        }

        String msgId = mqReceive.getMsgId();
        if (redissonService.tryLock(msgId)) {
            try {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{} --> {}", desc, msgId);
                }
                try {
                    // receive 失败重试时, 也是往 mq 里面发
                    mqSenderHandler.doProvideJustJson(mqInfo, null, mqReceive.getMsg());
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info("{} --> {} 成功", desc, msgId);
                    }
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("{} --> {} 异常", desc, msgId, e);
                    }
                }
            } finally {
                redissonService.unlock(msgId);
            }
            return;
        }

        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("{} --> {} 正在运行", desc, msgId);
        }
        // msgId 的数据只需要有一条在处理, 当前数据直接置为成功, 无需重试
        MqReceive update = new MqReceive();
        update.setId(mqReceive.getId());
        update.setStatus(2);
        update.setRemark(mqReceive.getRemark() + ";;同 msg_id 的任务正在执行");
        mqReceiveService.updateById(update);
    }

    public boolean handlerSend(String desc) {
        for (;;) {
            List<MqSend> mqSendList = mqSendService.queryRetryMsg(maxRetryCount, mqRetryLimit);
            if (A.isNotEmpty(mqSendList)) {
                return true;
            }
            for (MqSend mqSend : mqSendList) {
                retrySendSingle(desc, mqSend);
            }
            if (mqSendList.size() < mqRetryLimit) {
                return true;
            }
        }
    }

    private void retrySendSingle(String desc, MqSend mqSend) {
        // noinspection DuplicatedCode
        MqInfo mqInfo = MqInfo.from(mqSend.getBusinessType());
        if (U.isNull(mqInfo)) {
            // 没有 mq-info 的数据直接置为成功, 无法重试
            MqSend update = new MqSend();
            update.setId(mqSend.getId());
            update.setStatus(2);
            update.setRemark(mqSend.getRemark() + ";;没有这个 business_type 的场景");
            mqSendService.updateById(update);
            return;
        }

        String msgId = mqSend.getMsgId();
        if (redissonService.tryLock(msgId)) {
            try {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{} --> {}", desc, msgId);
                }
                try {
                    mqSenderHandler.doProvide(mqInfo, null, mqSend.getMsg());
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info("{} --> {} 成功", desc, msgId);
                    }
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("{} --> {} 异常", desc, msgId, e);
                    }
                }
            } finally {
                redissonService.unlock(msgId);
            }
            return;
        }

        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("{} --> {} 正在运行", desc, msgId);
        }
        // msgId 的数据只需要有一条在处理, 当前数据直接置为成功, 无需重试
        MqSend update = new MqSend();
        update.setId(mqSend.getId());
        update.setStatus(2);
        update.setRemark(mqSend.getRemark() + ";;同 msg_id 的任务正在执行");
        mqSendService.updateById(update);
    }
}
