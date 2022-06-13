package com.github.task;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.mq.constant.MqInfo;
import com.github.mq.handle.MqSenderHandler;
import com.github.mq.model.MqSend;
import com.github.mq.service.MqSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class RetryMqSendTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "重试 mq 发送";
    /** 当前任务的默认表达式 */
    private static final String CRON = "13 0/5 * * * *";

    @Value("mq.singleRetryCount:20")
    private int mqSingleRetryCount;

    @Value("mq.maxRetryCount:5")
    private int maxRetryCount;

    private final RedissonService redissonService;
    private final MqSendService mqSendService;
    private final MqSenderHandler mqSenderHandler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        DynamicCronUtil.runTask(schedule, BUSINESS_DESC, CRON, this::handlerBusiness);
    }

    /** 操作具体的业务 */
    public boolean handlerBusiness() {
        for (;;) {
            List<MqSend> mqSendList = mqSendService.queryRetryMsg(maxRetryCount, mqSingleRetryCount);
            if (A.isNotEmpty(mqSendList)) {
                return true;
            }
            retrySend(mqSendList);
            if (mqSendList.size() < mqSingleRetryCount) {
                return true;
            }
        }
    }

    private void retrySend(List<MqSend> mqSendList) {
        for (MqSend mqSend : mqSendList) {
            String msgId = mqSend.getMsgId();
            if (redissonService.tryLock(msgId)) {
                try {
                    MqInfo mqInfo = MqInfo.from(mqSend.getBusinessType());
                    if (U.isNotNull(mqInfo)) {
                        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                            LogUtil.ROOT_LOG.info("{} --> {}", BUSINESS_DESC, msgId);
                        }
                        try {
                            mqSenderHandler.doProvide(mqInfo, null, mqSend.getMsg());
                            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                                LogUtil.ROOT_LOG.info("{} --> {} 成功", BUSINESS_DESC, msgId);
                            }
                        } catch (Exception e) {
                            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                LogUtil.ROOT_LOG.error("{} --> {} 异常", BUSINESS_DESC, msgId, e);
                            }
                        }
                    } else {
                        // 没有 mq-info 的数据直接置为成功, 无法重试
                        MqSend update = new MqSend();
                        update.setId(mqSend.getId());
                        update.setStatus(2);
                        mqSendService.updateById(update);
                    }
                } finally {
                    redissonService.unlock(msgId);
                }
            }
        }
    }
}
