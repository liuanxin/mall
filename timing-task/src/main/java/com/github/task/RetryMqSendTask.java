package com.github.task;

import com.github.common.util.CronUtil;
import com.github.mq.handle.MqRetryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class RetryMqSendTask implements SchedulingConfigurer {

    // private final ConfigService configService;
    private final MqRetryHandler mqRetryHandler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        String cron = "13 0/2 * * * *"; // configService.getRetryMqCron();
        CronUtil.runTask(schedule, "重试 mq 发送", cron, mqRetryHandler::handlerSend);
    }
}
