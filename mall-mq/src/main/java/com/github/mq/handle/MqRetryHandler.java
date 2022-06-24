package com.github.mq.handle;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqInfo;
import com.github.mq.model.MqSend;
import com.github.mq.service.MqSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class MqRetryHandler {

    @Value("${mq.retryLimit:20}")
    private int mqRetryLimit;

    @Value("${mq.maxRetryCount:5}")
    private int maxRetryCount;

    private final RedissonService redissonService;
    private final MqSendService mqSendService;
    private final MqSenderHandler mqSenderHandler;

    /** 处理发送重试(将失败的重发到队列) */
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

        if (sendMsg(mqSend.getMsgId(), desc, mqSend.getSearchKey(), mqInfo, mqSend.getMsg())) {
            return;
        }

        // msgId 的数据只需要有一条在处理, 当前数据直接置为成功, 无需重试
        MqSend update = new MqSend();
        update.setId(mqSend.getId());
        update.setStatus(2);
        update.setRemark(mqSend.getRemark() + ";;同 msg_id 的任务正在执行");
        mqSendService.updateById(update);
    }

    private boolean sendMsg(String msgId, String desc, String searchKey, MqInfo mqInfo, String json) {
        if (redissonService.tryLock(msgId)) {
            try {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{} --> {}", desc, msgId);
                }
                mqSenderHandler.doProvideJustJson(msgId, mqInfo, searchKey, json);
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{} --> {} 成功", desc, msgId);
                }
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("{} --> {} 异常", desc, msgId, e);
                }
            } finally {
                redissonService.unlock(msgId);
            }
            return true;
        } else {
            return false;
        }
    }
}
