package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.captcha.CaptchaRecord;
import com.github.common.constant.CommonConst;
import com.github.common.json.JsonResult;
import com.github.common.util.Obj;
import com.github.global.config.CaptchaHandler;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.*;
import com.github.req.CaptchaVerifyReq;
import com.github.res.CaptchaRes;
import com.github.util.ManagerDataCollectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@ApiGroup(CommonConst.MODULE_INFO)
@RestController
@RequiredArgsConstructor
public class ManagerCommonController {

    private final CaptchaHandler captchaHandler;

    @ApiIgnore
    @ResponseBody
    @GetMapping("/")
    public String index() {
        return "manager-api";
    }

    @ApiTokens
    @ApiMethod(value = "枚举信息", develop = Develop.COMMON)
    @GetMapping("/enum")
    public JsonResult<Map<String, Object>> enumList(@ApiParam("枚举类型. 不传则返回所有列表, 多个以逗号分隔") String type) {
        return Obj.isBlank(type) ?
                JsonResult.success("枚举列表", ManagerDataCollectUtil.ALL_ENUM_INFO) :
                JsonResult.success("枚举信息", ManagerDataCollectUtil.singleEnumInfo(type));
    }

    /** width/height/dark 可选; dark=1|true|on 时深色主题, 否则浅色; image 为完整 data URI */
    @ApiMethod(value = "验证码", index = 1)
    @NotNeedLogin
    @GetMapping("/captcha")
    public JsonResult<CaptchaRes> captcha(
            @RequestParam(required = false) String width,
            @RequestParam(required = false) String height,
            @RequestParam(required = false) String dark
    ) {
        String id = Obj.uuid16();
        CaptchaRecord.Build build = captchaHandler.saveChallenge(id, width, height, dark);
        return JsonResult.success("验证码", CaptchaRes.assembly(id, build));
    }

    @ApiMethod(value = "验证码校验", index = 2)
    @NotNeedLogin
    @PostMapping("/captcha/verify")
    public JsonResult<String> captchaVerify(@RequestBody CaptchaVerifyReq req) {
        req.validate();
        String passToken = captchaHandler.verifyAndIssuePassToken(req.getId(), req.getPoints());
        return Obj.isBlank(passToken) ? JsonResult.fail("验证码错误") : JsonResult.success("验证通过", passToken);
    }
}
