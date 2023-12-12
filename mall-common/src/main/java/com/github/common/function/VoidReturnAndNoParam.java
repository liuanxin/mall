package com.github.common.function;

/**
 * <pre>
 * {@link VoidReturnAndNoParam} 无入参 + 无返回
 *
 * {@link java.util.function.Supplier}  无入参 + 有返回
 * {@link java.util.function.Consumer}  单入参 + 无返回
 * {@link java.util.function.Function}  单入参 + 有返回
 * {@link java.util.function.Predicate} 单入参 + 返回 boolean
 * {@link java.util.function.BiConsumer}  两个入参 + 无返回
 * {@link java.util.function.BiFunction}  两个入参 + 有返回
 * {@link java.util.function.BiPredicate} 两个入参 + 返回 boolean
 * </pre>
 */
public interface VoidReturnAndNoParam {

    void run();
}
