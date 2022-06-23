package com.github.task;

import com.github.mq.handle.MqRetryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class RetryMqSendTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "重试 mq 发送";
    /** 当前任务的默认表达式 */
    private static final String CRON = "13 0/2 * * * *";

    private final MqRetryHandler mqRetryHandler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        DynamicCronUtil.runTask(schedule, BUSINESS_DESC, CRON, mqRetryHandler::handlerSend);
    }
}
