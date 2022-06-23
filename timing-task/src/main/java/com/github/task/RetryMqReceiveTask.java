package com.github.task;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqInfo;
import com.github.mq.handle.MqSenderHandler;
import com.github.mq.model.MqReceive;
import com.github.mq.service.MqReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class RetryMqReceiveTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "重试 mq 消费";
    /** 当前任务的默认表达式 */
    private static final String CRON = "23 0/2 * * * *";

    @Value("mq.retryLimit:20")
    private int mqRetryLimit;

    @Value("mq.maxRetryCount:5")
    private int maxRetryCount;

    private final RedissonService redissonService;
    private final MqReceiveService mqReceiveService;
    private final MqSenderHandler mqSenderHandler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        DynamicCronUtil.runTask(schedule, BUSINESS_DESC, CRON, this::handlerBusiness);
    }

    /** 操作具体的业务 */
    public boolean handlerBusiness() {
        for (;;) {
            List<MqReceive> mqReceiveList = mqReceiveService.queryRetryMsg(maxRetryCount, mqRetryLimit);
            if (A.isNotEmpty(mqReceiveList)) {
                return true;
            }
            for (MqReceive mqReceive : mqReceiveList) {
                retrySend(mqReceive);
            }
            if (mqReceiveList.size() < mqRetryLimit) {
                return true;
            }
        }
    }

    private void retrySend(MqReceive mqReceive) {
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
                    LogUtil.ROOT_LOG.info("{} --> {}", BUSINESS_DESC, msgId);
                }
                try {
                    // receive 失败重试时, 也是往 mq 里面发
                    mqSenderHandler.doProvideJustJson(mqInfo, null, mqReceive.getMsg());
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info("{} --> {} 成功", BUSINESS_DESC, msgId);
                    }
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("{} --> {} 异常", BUSINESS_DESC, msgId, e);
                    }
                }
            } finally {
                redissonService.unlock(msgId);
            }
            return;
        }

        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("{} --> {} 正在运行", BUSINESS_DESC, msgId);
        }
        // msgId 的数据只需要有一条在处理, 当前数据直接置为成功, 无需重试
        MqReceive update = new MqReceive();
        update.setId(mqReceive.getId());
        update.setStatus(2);
        update.setRemark(mqReceive.getRemark() + ";;同 msg_id 的任务正在执行");
        mqReceiveService.updateById(update);
    }
}
