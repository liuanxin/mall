package com.github.global.aop;

import java.lang.annotation.*;

/** 自定义切面注解, 标了此注解的方法将会使用 {@link AopSelfAdvice} 进行处理 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AopSelf {
}
