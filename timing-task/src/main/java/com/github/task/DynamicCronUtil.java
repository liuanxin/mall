package com.github.task;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

import java.util.function.Function;

public class DynamicCronUtil {

    /**
     * <pre>
     * import lombok.RequiredArgsConstructor;
     * import org.springframework.context.annotation.Configuration;
     * import org.springframework.scheduling.annotation.SchedulingConfigurer;
     * import org.springframework.scheduling.config.ScheduledTaskRegistrar;
     *
     * &#064;Configuration
     * &#064;RequiredArgsConstructor
     * &#064;SuppressWarnings("NullableProblems")
     * public class XxxDynamicTask implements SchedulingConfigurer {
     *
     *     private final XxxService xxxService;
     *
     *     &#064;Override
     *     public void configureTasks(ScheduledTaskRegistrar schedule) {
     *         String desc = "xxx";
     *         String cron = "0 0 0/1 * * *";
     *         DynamicCronUtil.runTask(schedule, desc, cron, this::handlerBusiness);
     *     }
     *
     *     // 操作具体的业务
     *     private boolean handlerBusiness() {
     *         // xxxService.xxx();
     *         return true;
     *     }
     * }
     * </pre>
     *
     * @param desc 当前定时任务的业务说明
     * @param cron 定时任务的表达式
     * @param func 入参是任务的名称, 出参是 boolean(表示运行是否成功)的方法
     */
    public static void runTask(ScheduledTaskRegistrar schedule, String desc, String cron, Function<String, Boolean> func) {
        try {
            CronExpression.parse(cron);
        } catch (IllegalArgumentException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("定时任务({})的表达式({})有误", desc, cron, e);
            }
            return;
        }

        schedule.addTriggerTask(() -> {
            try {
                LogUtil.putTraceId(U.uuid16());
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("任务({})开始", desc);
                }
                boolean flag = func.apply(desc);
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("任务({})结束({})", desc, flag);
                }
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("任务({})异常", desc, e);
                }
            } finally {
                LogUtil.unbind();
            }
        }, (triggerContext) -> new CronTrigger(cron).nextExecutionTime(triggerContext));
    }
}
