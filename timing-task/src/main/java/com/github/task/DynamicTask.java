package com.github.task;

import com.github.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/** 动态设置运行时间的定时任务 --> 示例 */
@Configuration
@RequiredArgsConstructor
@SuppressWarnings("NullableProblems")
public class DynamicTask implements SchedulingConfigurer {

    private final ProductService productService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar schedule) {
        String desc = "下架商品";
        String cron = "0 0 0/1 * * *";
        DynamicCronUtil.runTask(schedule, desc, cron, this::handlerBusiness);
    }

    /** 操作具体的业务 */
    private boolean handlerBusiness() {
        // productService.xxx();
        return true;
    }
}

