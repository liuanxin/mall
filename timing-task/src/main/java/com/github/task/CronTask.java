package com.github.task;

import com.github.common.util.CronUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/** 定时任务 --> 示例 */
@Configuration
@RequiredArgsConstructor
public class CronTask {

    /** 当前定时任务的业务说明 */
    private static final String DESC = "定时示例";
    /** 当前任务的表达式 */
    private static final String CRON = "0/30 * * * * *";


    @Scheduled(cron = CRON)
    public void cancelOrder() {
        CronUtil.task(DESC, this::handlerBusiness);
    }

    /** 操作具体的业务 */
    private boolean handlerBusiness(String desc) {
        return true;
    }
}
