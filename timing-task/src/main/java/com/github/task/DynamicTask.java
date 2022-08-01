package com.github.task;

import com.github.common.util.CronUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/** 动态设置运行时间的定时任务 --> 示例 */
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class DynamicTask implements SchedulingConfigurer {

    // private final XxxService xxxService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        String cron = "0 0 0/1 * * *"; // 任务的运行表达式, 可以从 db 动态获取
        CronUtil.runTask(schedule, "任务说明", cron, this::handlerBusiness);
    }

    /** 操作具体的业务 */
    private boolean handlerBusiness(String desc) {
        // xxxService.doSomething();
        return true;
    }
}
