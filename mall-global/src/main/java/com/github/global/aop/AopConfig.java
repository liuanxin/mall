package com.github.global.aop;

import org.aspectj.lang.Aspects;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * <pre>
 * 加载了下面的包才开启 aop, 标了 @{@link AopSelf} 注解的方法使用 {@link AopSelfAdvice} 进行处理
 *
 * &lt;dependency&gt;
 *     &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *     &lt;artifactId&gt;spring-boot-starter-aop&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
@ConditionalOnClass(Aspects.class)
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

    @Bean
    public AnnotationMatchingPointcut aopSelfPointcut() {
        return new AnnotationMatchingPointcut(null, AopSelf.class, true);
    }

    @Bean
    public AopSelfAdvice aopSelfAdvice() {
        return new AopSelfAdvice();
    }

    @Bean
    public DefaultPointcutAdvisor aopSelfAdvisor(AnnotationMatchingPointcut aopSelfPointcut, AopSelfAdvice aopSelfAdvice) {
        return new DefaultPointcutAdvisor(aopSelfPointcut, aopSelfAdvice);
    }
}
