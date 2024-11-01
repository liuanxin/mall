package com.github.common;

/** union-type 结构体, 一次返回结果和异常, 正常数据和异常信息在调用方拿到结果后处理 */
public record Result<T, E> (T value, E error, boolean isOk) {

    public static <T, E> Result<T, E> ok(T value) {
        return new Result<>(value, null, true);
    }

    public static <T, E> Result<T, E> err(E error) {
        return new Result<>(null, error, false);
    }
}
