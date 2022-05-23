package com.github.task;

import com.github.common.date.DateUtil;
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
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RetryMqSendTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "重试 mq 发送";
    /** 当前任务的默认表达式 */
    private static final String CRON = "13 0/5 * * * *";

    @Value("mq.singleRetryCount:20")
    private int mqSingleRetryCount;

    private final RedissonService redissonService;
    private final MqSendService mqSendService;
    private final MqSenderHandler mqSenderHandler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Runnable task = () -> {
            long start = System.currentTimeMillis();
            try {
                LogUtil.bindBasicInfo(U.uuid16());
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{}开始", BUSINESS_DESC);
                }
                handlerBusiness();
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error(BUSINESS_DESC + "异常", e);
                }
            } finally {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("处理 job 结束({}), 耗时: ({})",
                            BUSINESS_DESC, DateUtil.toHuman(System.currentTimeMillis() - start));
                }
                LogUtil.unbind();
            }
        };

        Trigger trigger = (triggerContext) -> {
            // 从数据库读取 cron 表达式
            String cron = ""; // commonService.getAbcCron();
            if (U.isBlank(cron)) {
                // 如果没有, 给一个默认值.
                cron = CRON;
            }

            // 如果设置的表达式有误也使用默认的
            CronTrigger cronTrigger;
            try {
                cronTrigger = new CronTrigger(cron);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("{}的表达式有误, 使用默认值({})", BUSINESS_DESC, CRON, e);
                }
                cronTrigger = new CronTrigger(CRON);
            }
            return cronTrigger.nextExecutionTime(triggerContext);
        };
        taskRegistrar.addTriggerTask(task, trigger);
    }

    /** 操作具体的业务 */
    private void handlerBusiness() {
        for (;;) {
            List<MqSend> mqSendList = mqSendService.queryRetryMsg(5, mqSingleRetryCount);
            if (A.isNotEmpty(mqSendList)) {
                return;
            }
            retrySend(mqSendList);
            if (mqSendList.size() < mqSingleRetryCount) {
                return;
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
