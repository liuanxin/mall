package com.github.common.function;

/**
 * <pre>
 * 当想要表现多(2 ~ 9)类型时使用, 比如 2 个类型
 *
 * 返回
 * public Two&lt;Long, String&gt; twoReturn() {
 *     ...
 *     return new Two&lt;&gt;(123L, "");
 * }
 *
 * 使用
 * Two&lt;Long, String&gt; data = twoReturn();
 * Long id = data.one();
 * String no = data.two();
 * </pre>
 */
public class Multi {

    public record Two<O, T>(O one, T two) {}

    public record Three<O, T, H>(O one, T two, H three) {}

    public record Four<O, T, H, F>(O one, T two, H three, F four) {}

    public record Five<O, T, H, F, V>(O one, T two, H three, F four, V five) {}

    public record Six<O, T, H, F, V, S>(O one, T two, H three, F four, V five, S six) {}

    public record Seven<O, T, H, F, V, S, N>(O one, T two, H three, F four, V five, S six, N seven) {}

    public record Eight<O, T, H, F, V, S, N, E>(O one, T two, H three, F four, V five, S six, N seven, E eight) {}

    public record Nine<O, T, H, F, V, S, N, E, I>(O one, T two, H three, F four, V five, S six, N seven, E eight, I nine) {}
}
