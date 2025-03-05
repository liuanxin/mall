package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;

/**
 * 返回码. 前端基于此进行相应的页面跳转, 通常会有 渲染数据、输出返回描述、导到登录页、不进行任务处理 这几种
 *
 * @see org.springframework.http.HttpStatus
 */
public enum JsonCode {

    // 一般来说, 返回编码就用在 http response code 上就好了, 当需要前端来变更页面逻辑时才需要添加

    /** 200: 将 data 解析后渲染页面(依业务而定, 也可能显示 msg 给用户看, 如 收货地址添加成功 这种) */
    SUCCESS(200, "成功"),

    /** 400: 参数有误(客户端错误) */
    BAD_REQUEST(400, "参数有误"),

    /** 401: 未登录(客户端错误) */
    NOT_LOGIN(401, "未登录"),

    /** 403: 无权限(客户端错误) */
    NOT_PERMISSION(403, "无权限"),

    /** 404 (客户端错误) */
    NOT_FOUND(404, "未找到相应处理"),

    /** 500: 内部错误、业务异常(服务端错误) */
    FAIL(500, "内部错误、业务异常")

    // /** 1000: xxx 业务异常 */
    // , SERVICE_FAIL(1000, "xxx 业务异常")
    ;

    private final int code;
    private final String value;

    JsonCode(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }
    public String getValue() {
        return value;
    }

    @JsonValue
    public int serializer() {
        return code;
    }
    @JsonCreator
    public static JsonCode deserializer(Object obj) {
        if (U.isNotNull(obj)) {
            String str = obj.toString().trim();
            for (JsonCode e : values()) {
                if (str.equals(String.valueOf(e.code))) {
                    return e;
                }
            }
        }
        return null;
    }
}
