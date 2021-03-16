package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** <span style="color:red;">!!!此实体类请只在 Controller 中使用, 且只调用其 static 方法!!!</span> */
@Setter
@Getter
@NoArgsConstructor
public class JsonResult<T> {

    // 1. status 返回 200 400 404 500 码, 实体返回 msg
    // 2. status 全部返回 200, 实体里面返回 json, json 中返回 200 400 500 这些码
    // 使用上面哪一种方式? 这里是有争议的, 如果使用前者, 需要将下面的 JsonCode code 注起来并使用 GlobalException2, 去掉 GlobalException
    @ApiReturn("返回码")
    private JsonCode code;

    @ApiReturn(value = "返回说明", example = "用户名密码错误 | 收货地址添加成功")
    private String msg;

    @ApiReturn(value = "错误信息", example = "当非生产时返回")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errorList;

    @ApiReturn("返回数据, 实体 {\"id\":1} | 列表 [{\"id\":1},{\"id\":2}] 看具体的业务")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private T data;

    /*
    // 这里只在登录接口那里返回
    @ApiReturn("需要 app 保存到本地的值(pc 无视), 每次请求都带上, key 是" + Const.TOKEN + ", header 或 param 都可")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String token;
    */

    private JsonResult(JsonCode code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    private JsonResult(JsonCode code, String msg, List<String> errorList) {
        this(code, msg);
        this.errorList = errorList;
    }
    private JsonResult(JsonCode code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }


    // ---------- 在 controller 中请只使用下面的静态方法就好了. 不要 new JsonResult()... 这样操作 ----------

    public static <T> JsonResult<T> success(String msg) {
        return new JsonResult<>(JsonCode.SUCCESS, msg);
    }

    public static <T> JsonResult<T> success(String msg, T data) {
        return new JsonResult<>(JsonCode.SUCCESS,msg, data);
    }


    public static <T> JsonResult<T> badRequest(String msg) {
        return new JsonResult<>(JsonCode.BAD_REQUEST, msg);
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
    public static <T> JsonResult<T> fail(String msg, List<String> errorList) {
        return new JsonResult<>(JsonCode.FAIL, msg, errorList);
    }
}
