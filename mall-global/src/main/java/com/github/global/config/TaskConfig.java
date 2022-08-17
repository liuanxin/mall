package com.github.global.config;

import com.github.common.json.JsonUtil;
import com.github.common.util.AsyncUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${asyncTask.corePoolSize:0}")
    private int corePoolSize;

    @Value("${asyncTask.maxPoolSize:0}")
    private int maxPoolSize;

    @Value("${asyncTask.queueCapacity:0}")
    private int queueCapacity;

    /**
     * @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties
     * @see org.springframework.aop.interceptor.AsyncExecutionInterceptor#determineAsyncExecutor
     */
    @SuppressWarnings({"JavadocReference", "NullableProblems"})
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 如果 cpu 核心是   1, 则最前面的   1 个异步处理, 接下来的   1024 个(左移 10 位 = * 1024)放进队列, 之后的 2 个(   1 + 2 -   1)异步处理, 再往后的拒绝
        // 如果 cpu 核心是   2, 则最前面的   1 个异步处理, 接下来的   2048 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(   2 + 2 -   1)异步处理, 再往后的拒绝
        // 如果 cpu 核心是   4, 则最前面的   3 个异步处理, 接下来的   4096 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(   4 + 2 -   3)异步处理, 再往后的拒绝
        // 如果 cpu 核心是   8, 则最前面的   7 个异步处理, 接下来的   8192 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(   8 + 2 -   7)异步处理, 再往后的拒绝
        // 如果 cpu 核心是  16, 则最前面的  15 个异步处理, 接下来的  16384 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(  16 + 2 -  15)异步处理, 再往后的拒绝
        // 如果 cpu 核心是  32, 则最前面的  31 个异步处理, 接下来的  32768 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(  32 + 2 -  31)异步处理, 再往后的拒绝
        // 如果 cpu 核心是  64, 则最前面的  63 个异步处理, 接下来的  65536 个(左移 10 位 = * 1024)放进队列, 之后的 3 个(  64 + 2 -  63)异步处理, 再往后的拒绝
        // 如果 cpu 核心是 128, 则最前面的 127 个异步处理, 接下来的 131072 个(左移 10 位 = * 1024)放进队列, 之后的 3 个( 128 + 2 - 127)异步处理, 再往后的拒绝
        // 如果 cpu 核心是 256, 则最前面的 255 个异步处理, 接下来的 262144 个(左移 10 位 = * 1024)放进队列, 之后的 3 个( 256 + 2 - 255)异步处理, 再往后的拒绝
        executor.setCorePoolSize(corePoolSize > 0 ? corePoolSize : Math.max(U.PROCESSORS - 1, 1));
        executor.setMaxPoolSize(maxPoolSize > 0 ? maxPoolSize : (U.PROCESSORS + 2));
        executor.setQueueCapacity(queueCapacity > 0 ? queueCapacity : (U.PROCESSORS << 10));
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
