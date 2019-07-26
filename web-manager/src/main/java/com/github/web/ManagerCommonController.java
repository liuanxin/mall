package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.constant.CommonConst;
import com.github.common.json.JsonResult;
import com.github.common.util.U;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.util.ManagerDataCollectUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@NotNeedLogin
@ApiGroup(CommonConst.MODULE_INFO)
@RestController
public class ManagerCommonController {

    @ApiMethod(value = "枚举数据", develop = Develop.COMMON)
    @GetMapping("/enum")
    public JsonResult enumList(@ApiParam("枚举类型. 不传则返回列表, type 与 枚举的类名相同, 忽略大小写") String type) {
        return U.isBlank(type) ?
                JsonResult.success("枚举列表", ManagerDataCollectUtil.ALL_ENUM_INFO) :
                JsonResult.success("枚举信息", ManagerDataCollectUtil.singleEnumInfo(type));
    }
}
