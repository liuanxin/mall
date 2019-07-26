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

    /** 将 data 解析后渲染页面(依业务而定, 也可能显示 msg 给用户看, 如 收货地址添加成功 这种) */
    SUCCESS(200, "成功"),

    // 跟 500 的处理方式是一致的
    // BAD_REQUEST(400, "参数有误(输出 msg 即可)"),

    /** 导向登录页面引导用户登录 */
    NOT_LOGIN(401, "未登录"),

    // 跟 500 的处理方式是一致的
    // NOT_PERMISSION(403, "无权限"),

    NOT_FOUND(404, "404"),

    /** 显示 msg 给用户看 */
    FAIL(500, "内部错误或业务异常");

    int code;
    String value;
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
        JsonCode code = U.enumDeserializer(obj, JsonCode.class);
        return U.isBlank(code) ? SUCCESS : code;
    }
}
