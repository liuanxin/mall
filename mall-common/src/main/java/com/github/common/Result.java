package com.github.common;

/**
 * <pre>
 * union-type 结构体, 一次返回结果和异常, 正常数据和异常信息在调用方拿到结果后处理
 *
 * 正常返回时: Result&lt;Xxx, Exception> r = Result.ok(xxx);
 * 异常返回时: Result&lt;Xxx, Exception> r = Result.err(e);
 *
 * 用的时候:
 * if (r.isError()) {
 *     return Result.err(r.error());
 * }
 * Xxx x = r.data();
 * </pre>
 */
public record Result<T, E extends Throwable> (T data, E error, boolean isError) {

    public static <T, E extends Throwable> Result<T, E> ok(T data) {
        return new Result<>(data, null, false);
    }

    public static <T, E extends Throwable> Result<T, E> err(E error) {
        return new Result<>(null, error, true);
    }
}
