package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.json.JsonResult;
import com.github.common.util.CaptchaUtil;
import com.github.common.util.Obj;
import com.github.global.config.CaptchaHandler;
import com.github.liuanxin.api.annotation.ApiIgnore;
import com.github.req.CaptchaVerifyReq;
import com.github.res.CaptchaRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@ApiIgnore
@NotNeedLogin
@Controller
@RequiredArgsConstructor
public class ManagerIndexController {

    private final CaptchaHandler captchaHandler;

    @ResponseBody
    @GetMapping("/")
    public String index() {
        return "manager-api";
    }


    @GetMapping("/captcha")
    public JsonResult<CaptchaRes> captcha(
            @RequestParam(required = false) String width,
            @RequestParam(required = false) String height,
            @RequestParam(required = false) String dark
    ) {
        CaptchaUtil.CaptchaBuild build = CaptchaUtil.buildClickCaptcha(width, height, dark);
        String id = Obj.uuid16();
        captchaHandler.saveChallenge(id, build.challenge());
        return JsonResult.success("中文点选验证码", CaptchaRes.assembly(id, build));
    }

    @NotNeedLogin
    @PostMapping("/captcha/verify")
    public JsonResult<String> captchaVerify(@RequestBody CaptchaVerifyReq req) {
        req.validate();
        String passToken = captchaHandler.verifyAndIssuePassToken(req.getId(), req.getPoints());
        return Obj.isBlank(passToken) ? JsonResult.fail("验证码错误") : JsonResult.success("验证通过", passToken);
    }
}
