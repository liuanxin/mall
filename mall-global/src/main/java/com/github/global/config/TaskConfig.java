package com.github.global.config;

import com.github.common.util.A;
import com.github.common.util.U;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 让 @Async 注解下的方法异步处理的线程池. 下面的代码相当于以下的配置<br><br>
 * &lt;task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/&gt;<br>
 * &lt;task:executor id="myExecutor" pool-size="4-32" queue-capacity="8"/&gt;<br>
 * &lt;context id="exceptionHandler" class="...SimpleAsyncUncaughtExceptionHandler"/&gt;<br><br>
 *
 * 如果想异步处理后返回结果, 在 @Async 注解的方法返回上使用 Future<>, 方法体里面的返回使用 new AsyncResult<>()
 */
@Configuration
@EnableAsync
public class TaskConfig implements AsyncConfigurer {

    /** @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 如果 cpu 核心是 8, 则最前面的 16 个异步处理, 后面的 1984 个放进队列, 之后的 16 个(32 - 16)异步处理, 再往后的拒绝
        executor.setCorePoolSize(U.PROCESSORS << 1);     // 8 * 2 = 16
        executor.setMaxPoolSize(U.PROCESSORS << 2);      // 8 * 4 = 32
        executor.setQueueCapacity((U.PROCESSORS << 8) - (U.PROCESSORS << 3)); // (8 * 256) - (8 * 8) = 1984
        executor.setThreadNamePrefix("task-executor-");  // 线程名字的前缀
        // 见: https://moelholm.com/blog/2017/07/24/spring-43-using-a-taskdecorator-to-copy-mdc-data-to-async-threads
        executor.setTaskDecorator(runnable -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            boolean hasRequest = (attributes instanceof ServletRequestAttributes);

            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            // 把主线程运行时的请求和日志上下文放到 异步任务的请求和日志上下文去
            return () -> {
                try {
                    if (hasRequest) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    MDC.setContextMap(A.isEmpty(contextMap) ? Collections.emptyMap() : contextMap);
                    runnable.run();
                } finally {
                    if (hasRequest) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    MDC.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
