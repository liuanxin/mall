package com.github.global.config;

import com.github.common.util.U;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 如果 cpu 核心是 8 的话
        executor.setCorePoolSize(U.PROCESSORS << 1);     // 8 * 2 = 16
        executor.setMaxPoolSize(U.PROCESSORS << 3);      // 8 * 8 = 64
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("task-executor-");  // 线程名字的前缀
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
