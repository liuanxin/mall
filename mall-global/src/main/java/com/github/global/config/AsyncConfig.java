package com.github.global.config;

import com.github.common.json.JsonUtil;
import com.github.common.util.AsyncUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@SuppressWarnings("JavadocReference")
@Configuration
@EnableAsync
@ConditionalOnClass(AsyncConfigurer.class)
public class AsyncConfig implements AsyncConfigurer {

    /** @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Pool#coreSize */
    @Value("${asyncTask.core-pool-size:0}")
    private int corePoolSize;

    /** @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Pool#maxSize */
    @Value("${asyncTask.max-pool-size:0}")
    private int maxPoolSize;

    /** @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties.Pool#queueCapacity */
    @Value("${asyncTask.queue-capacity:0}")
    private int queueCapacity;

    /**
     * @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties
     * @see org.springframework.aop.interceptor.AsyncExecutionInterceptor#determineAsyncExecutor
     */
    @SuppressWarnings({"JavadocReference", "NullableProblems"})
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int poolSize = U.calcPoolSize(0.8, 20, 180);
        executor.setCorePoolSize(corePoolSize > 0 ? corePoolSize : poolSize);
        executor.setMaxPoolSize(maxPoolSize > 0 ? maxPoolSize : (poolSize + 1));
        executor.setQueueCapacity(queueCapacity > 0 ? queueCapacity : (U.CPU_SIZE << 11));
        executor.setThreadNamePrefix("task-executor-");
        // 见: https://moelholm.com/blog/2017/07/24/spring-43-using-a-taskdecorator-to-copy-mdc-data-to-async-threads
        executor.setTaskDecorator(AsyncUtil::wrapRunContext);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("调用异步方法({})参数({})时异常", method, U.toStr(JsonUtil.toJsonNil(params)), ex);
            }
        };
    }
}
