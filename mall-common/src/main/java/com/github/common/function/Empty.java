package com.github.common.function;

/**
 * <pre>
 * 无入参 + 无返回 用 {@link Empty}
 *
 * 单入参 + 无返回 用 {@link java.util.function.Consumer}
 * 单入参 + 有返回 用 {@link java.util.function.Function}
 * 单入参 + 返回 boolean 用 {@link java.util.function.Predicate}
 * 无入参 + 有返回 用 {@link java.util.function.Supplier}
 *
 * 两个入参 + 无返回 用 {@link java.util.function.BiConsumer}
 * 两个入参 + 有返回 用 {@link java.util.function.BiFunction}
 * 两个入参 + 返回 boolean 用 {@link java.util.function.BiPredicate}
 * </pre>
 */
public interface Empty {

    void run();
}
