package com.github.common.util;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.*;

public final class AsyncUtil {

    public static ExecutorService ioExecutor(int queueSize) {
        int poolSize = U.calcPoolSize(0.75, 10, 190);
        return new ThreadPoolExecutor(
                poolSize, poolSize + 1,
                60L, TimeUnit.SECONDS,
                (queueSize > 0 ? new LinkedBlockingQueue<>(queueSize) : new SynchronousQueue<>()),
                wrapThreadFactory()
        );
    }
    /** IO 密集型的线程池(时间都消耗在了磁盘、网络上), 线程数可以大一点, 队列数可以小一点 */
    public static ExecutorService ioExecutor() {
        return ioExecutor(U.CPU_SIZE << 14); // * 16384
    }

    public static ExecutorService cpuExecutor(int queueSize) {
        // core: CPU 核心数,  max: CPU 核心 + 1,  queue: CPU 核心 * 2048
        int cpuSize = U.CPU_SIZE;
        return new ThreadPoolExecutor(
                cpuSize, cpuSize + 1,
                60L, TimeUnit.SECONDS,
                (queueSize > 0 ? new LinkedBlockingQueue<>(queueSize) : new SynchronousQueue<>()),
                wrapThreadFactory()
        );
    }
    /** CPU 密集型的线程池(时间都消耗在了计算上), 线程数保持跟 cpu 核数一样, 队列数可以大一点 */
    public static ExecutorService cpuExecutor() {
        return cpuExecutor(U.CPU_SIZE << 15); // * 32768
    }

    /**
     * 直接用 Executors 的静态方法生成的线程池, 想要线程池里的线程共享主线程的上下文, 构造时使用此方法生成的 ThreadFactory<p/>
     *
     * 如 <code>Executors.newSingleThreadExecutor(AsyncUtil.wrapThreadFactory())</code>
     */
    public static ThreadFactory wrapThreadFactory() {
        return runnable -> new Thread(wrapRunContext(runnable));
    }

    /** 线程想要共享主线程的上下文, 使用此方法 */
    public static Runnable wrapRunContext(Runnable runnable) {
        // noinspection DuplicatedCode
        if (U.isNull(runnable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> logContextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(logContextMap);

        if (hasWeb || hasLogContext) {
            // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
            return () -> {
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(logContextMap);
                    }
                    if (hasWeb) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    runnable.run();
                } finally {
                    if (hasWeb) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (hasLogContext) {
                        MDC.clear();
                    }
                }
            };
        } else {
            return runnable;
        }
    }

    /** 回调线程想要共享主线程的上下文, 使用此方法 */
    public static <T> Callable<T> wrapCallContext(Callable<T> callable) {
        // noinspection DuplicatedCode
        if (U.isNull(callable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> logContextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(logContextMap);

        if (hasWeb || hasLogContext) {
            return () -> {
                // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(logContextMap);
                    }
                    if (hasWeb) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    return callable.call();
                } finally {
                    if (hasWeb) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (hasLogContext) {
                        MDC.clear();
                    }
                }
            };
        } else {
            return callable;
        }
    }
}
