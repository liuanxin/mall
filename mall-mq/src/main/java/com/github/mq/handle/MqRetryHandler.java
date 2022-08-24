package com.github.mq.handle;

import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqConst;
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

    @Value("${mq.retryLimit:20}")
    private int mqRetryLimit;

    @Value("${mq.maxRetryCount:5}")
    private int maxRetryCount;

    private final RedissonService redissonService;
    private final MqSendService mqSendService;
    private final MqReceiveService mqReceiveService;
    private final MqSenderHandler mqSenderHandler;


    /**
     * 处理消费重试(将失败的重发到队列), 可以使用自带的 spring.rabbitmq.listener.simple.retry.enabled 方式进行自动重试
     *
     * @see com.github.mq.handle.MqReceiverHandler
     */
    public boolean handlerReceive(String desc) {
        for (;;) {
            List<MqReceive> mqReceiveList = mqReceiveService.queryRetryMsg(maxRetryCount, mqRetryLimit);
            if (A.isEmpty(mqReceiveList)) {
                return true;
            }
            for (MqReceive mqReceive : mqReceiveList) {
                retryReceive(desc, mqReceive);
            }
            // 如果上面查到的已经是最后一批数据也退出循环, 这样当上面的处理失败, 将会在下一次运行时执行
            if (mqReceiveList.size() < mqRetryLimit) {
                return true;
            }
        }
    }
    private void retryReceive(String desc, MqReceive mqReceive) {
        String oldRemark = U.toStr(mqReceive.getRemark());
        int status = MqConst.SUCCESS;
        String remark = null;
        try {
            MqInfo mqInfo = MqInfo.from(mqReceive.getType());
            if (U.isNull(mqInfo)) {
                remark = String.format("<%s : 没有这个业务类型场景>%s", DateUtil.nowDateTime(), oldRemark);
            } else {
                if (sendMsg(mqReceive.getMsgId(), desc, mqReceive.getSearchKey(), mqInfo, mqReceive.getMsg())) {
                    remark = String.format("<%s : 重试时发到 mq 成功>%s", DateUtil.nowDateTime(), oldRemark);
                } else {
                    remark = String.format("<%s : 同 msg_id 的任务正在执行>%s", DateUtil.nowDateTime(), oldRemark);
                }
            }
        } catch (Exception e) {
            status = MqConst.FAIL;
        } finally {
            MqReceive update = new MqReceive();
            update.setId(mqReceive.getId());
            update.setStatus(status);
            if (status == MqConst.FAIL) {
                update.setRetryCount(mqReceive.getRetryCount() + 1);
            }
            if (U.isNotBlank(remark)) {
                update.setRemark(remark);
            }
            // 状态变更: 成功 或 失败且重试次数 + 1
            mqReceiveService.updateById(update);
        }
    }

    /** 处理发送重试(将失败的重发到队列) */
    public boolean handlerSend(String desc) {
        for (;;) {
            List<MqSend> mqSendList = mqSendService.queryRetryMsg(maxRetryCount, mqRetryLimit);
            if (A.isEmpty(mqSendList)) {
                return true;
            }
            for (MqSend mqSend : mqSendList) {
                retrySend(desc, mqSend);
            }
            // 如果上面查到的已经是最后一批数据也退出循环, 这样当上面的处理失败, 将会在下一次运行时执行
            if (mqSendList.size() < mqRetryLimit) {
                return true;
            }
        }
    }
    private void retrySend(String desc, MqSend mqSend) {
        String oldRemark = U.toStr(mqSend.getRemark());
        int status = MqConst.SUCCESS;
        String remark = null;
        try {
            MqInfo mqInfo = MqInfo.from(mqSend.getType());
            if (U.isNull(mqInfo)) {
                remark = String.format("<%s : 没有这个业务类型场景>%s", DateUtil.nowDateTime(), oldRemark);
            } else {
                if (!sendMsg(mqSend.getMsgId(), desc, mqSend.getSearchKey(), mqInfo, mqSend.getMsg())) {
                    remark = String.format("<%s : 同 msg_id 的任务正在执行>%s", DateUtil.nowDateTime(), oldRemark);
                }
            }
        } catch (Exception e) {
            status = MqConst.FAIL;
        } finally {
            // 上面的 sendMsg 调用返回为 true 时, 内部方法会自动处理「成功」或「失败且重试次数 + 1」, 因此当前只处理 remark 有值的场景
            if (U.isNotBlank(remark)) {
                MqSend update = new MqSend();
                update.setId(mqSend.getId());
                update.setStatus(status);
                if (status == MqConst.FAIL) {
                    update.setRetryCount(mqSend.getRetryCount() + 1);
                }
                update.setRemark(remark);
                mqSendService.updateById(update);
            }
        }
    }

    private boolean sendMsg(String msgId, String desc, String searchKey, MqInfo mqInfo, String json) {
        if (redissonService.tryLock(msgId)) {
            try {
                mqSenderHandler.doProvideJustJson(msgId, mqInfo, searchKey, json);
            } finally {
                redissonService.unlock(msgId);
            }
            return true;
        } else {
            return false;
        }
    }
}
