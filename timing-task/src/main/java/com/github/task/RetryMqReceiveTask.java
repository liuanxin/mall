//package com.github.task;
//
//import com.github.mq.handle.MqRetryHandler;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.SchedulingConfigurer;
//import org.springframework.scheduling.config.ScheduledTaskRegistrar;
//
//@Configuration
//@RequiredArgsConstructor
//@SuppressWarnings("NullableProblems")
//public class RetryMqReceiveTask implements SchedulingConfigurer {
//
//    /** 当前定时任务的业务说明 */
//    private static final String BUSINESS_DESC = "重试 mq 消费";
//    /** 当前任务的默认表达式 */
//    private static final String RECEIVE_CRON = "23 0/2 * * * *";
//
//    private final MqRetryHandler mqRetryHandler;
//
//    @Override
//    public void configureTasks(ScheduledTaskRegistrar schedule) {
//        DynamicCronUtil.runTask(schedule, BUSINESS_DESC, RECEIVE_CRON, mqRetryHandler::handlerReceive);
//    }
//}
