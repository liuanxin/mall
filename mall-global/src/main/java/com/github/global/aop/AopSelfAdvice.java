package com.github.global.aop;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** 用来处理 {@link AopSelf} 注解的拦截器 */
public class AopSelfAdvice implements MethodInterceptor {

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        try {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("Before call aop -> method({}) args({})",
                        invocation.getMethod(), A.toStr(invocation.getArguments(), "<", ">"));
            }
            return invocation.proceed();
        } finally {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("After call aop");
            }
        }
    }
}
