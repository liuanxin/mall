package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.annotation.NotNeedPermission;
import com.github.common.json.JsonResult;
import com.github.common.util.U;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.manager.service.ManagerService;
import com.github.user.constant.UserConst;
import com.github.util.ManagerSessionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiGroup(UserConst.MODULE_INFO)
@RestController
@RequestMapping("/user")
public class ManagerUserController {

    private final ManagerService managerService;
    public ManagerUserController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @NotNeedLogin
    @ApiMethod(value = "登录", develop = Develop.USER, index = 1)
    @GetMapping("/login")
    public JsonResult login(@ApiParam(value = "用户名", must = true) String username,
                            @ApiParam(value = "用户名", must = true) String password,
                            @ApiParam("验证码") String code) {
        U.assertNil(username, "请输入用户名");
        U.assertNil(password, "请输入密码");
        // 登录: 1.输错 3 次密码就需要验证码, 2.登录成功就清除缓存中的次数, 失败就自增次数(失效时间为当天)
        // User user = managerService.login(username, password, code);
        // ManagerSessionUtil.whenLogin(user);
        // return JsonResult.success("登录成功", UserVo.assemblyData(user));
        return null;
    }

    @ApiMethod(value = "退出", develop = Develop.USER, index = 2)
    @GetMapping("/logout")
    public JsonResult logout() {
        ManagerSessionUtil.signOut();
        return null;
    }

    // 这个接口不需要验证权限(只需要登录就可以了)
    @NotNeedPermission
    @ApiMethod(value = "个人信息", develop = Develop.USER)
    @GetMapping("/info")
    public JsonResult userInfo() {
        return null;
    }
}
