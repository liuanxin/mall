package com.github.common;

/** union-type 结构体, 来达到一次返回结果和异常 */
public class Result<T, E> {

    private final T value;
    private final E error;
    private final boolean isOk;

    private Result(T value, E error, boolean isOk) {
        this.value = value;
        this.error = error;
        this.isOk = isOk;
    }

    public boolean isOk() {
        return isOk;
    }
    public T getValue() {
        return value;
    }
    public E getError() {
        return error;
    }


    public static <T, E> Result<T, E> ok(T value) {
        return new Result<>(value, null, true);
    }

    public static <T, E> Result<T, E> err(E error) {
        return new Result<>(null, error, false);
    }
}
