package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JsonResult<T> {

    // 1. status 返回 200 400 404 500 码, 200 时返回 json, 非 200 时返回 msg
    // 2. status 全部返回 200, 返回 json 里的 code 使用 200 400 500 这些码
    // 使用上面哪一种方式? 这里是有争议的, 如果使用前者则需要将 JsonCode code 注起来,
    // 并使用 GlobalException2, 去掉 GlobalException
    @ApiReturn("返回码")
    private JsonCode code;

    @ApiReturn(value = "返回说明", example = "用户名密码错误 | 收货地址添加成功")
    private String msg;

    @ApiReturn("返回数据, 实体 {\"id\":1} | 列表 [{\"id\":1},{\"id\":2}] 看具体的业务")
    private T data;

    @ApiReturn("跟踪 id")
    private String traceId;

    @ApiReturn(value = "验证失败信息, 返回 { 入参 1 : 错误信息 1, 入参 2 : 错误信息 2 }")
    private Map<String, String> validate;

    @ApiReturn(value = "错误信息, 只在非生产时返回")
    private List<String> error;

    private JsonResult(JsonCode code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    private JsonResult(JsonCode code, String msg, Map<String, String> validate) {
        this(code, msg);
        this.validate = validate;
    }
    private JsonResult(JsonCode code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }

    public String getTraceId() {
        return U.defaultIfBlank(traceId, LogUtil.getTraceId());
    }


    // ---------- 在 controller 中请只使用下面的静态方法就好了. 不要 new JsonResult()... 这样操作 ----------

    public static <T> JsonResult<T> success(String msg) {
        return new JsonResult<>(JsonCode.SUCCESS, msg);
    }

    public static <T> JsonResult<T> success(String msg, T data) {
        return new JsonResult<>(JsonCode.SUCCESS,msg, data);
    }


    public static <T> JsonResult<T> badRequest(String msg, Map<String, String> validate) {
        return new JsonResult<>(JsonCode.BAD_REQUEST, msg, validate);
    }

    public static <T> JsonResult<T> needLogin(String msg) {
        return new JsonResult<>(JsonCode.NOT_LOGIN, msg);
    }

    public static <T> JsonResult<T> needPermission(String msg) {
        return new JsonResult<>(JsonCode.NOT_PERMISSION, msg);
    }

    public static <T> JsonResult<T> notFound(String msg) {
        return new JsonResult<>(JsonCode.NOT_FOUND, msg);
    }

    public static <T> JsonResult<T> fail(String msg) {
        return new JsonResult<>(JsonCode.FAIL, msg);
    }
}
