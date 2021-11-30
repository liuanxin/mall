package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.annotation.NotNeedPermission;
import com.github.common.encrypt.Encrypt;
import com.github.common.json.JsonResult;
import com.github.common.util.FileUtil;
import com.github.common.util.U;
import com.github.config.ManagerConfig;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.manager.model.ManagerUser;
import com.github.manager.service.ManagerService;
import com.github.res.ManagerUserRes;
import com.github.user.constant.UserConst;
import com.github.util.ManagerSessionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@ApiGroup(UserConst.MODULE_INFO)
@RestController
@RequestMapping("/user")
public class ManagerUserController {


    private final ManagerConfig config;
    private final ManagerService adminService;
    public ManagerUserController(ManagerConfig config, ManagerService adminService) {
        this.config = config;
        this.adminService = adminService;
    }

    @NotNeedLogin
    @ApiMethod(value = "登录", index = 0)
    @PostMapping("/login")
    public JsonResult<ManagerUserRes> login(@ApiParam("用户名") String userName, @ApiParam("密码") String password) {
        U.assertException(U.isEmpty(userName) || U.isEmpty(password), "请输入用户名或密码");

        ManagerUser user = adminService.getUser(userName);
        U.assertNil(user, "用户名或密码有误");
        U.assertException(Encrypt.checkNotBcrypt(password, user.getPassword()), "用户名或密码不正确");
        U.assertException(U.isTrue(user.getStatus()), "用户被禁止登录, 请联系管理员");

        // 登录成功后填充菜单和权限, 平级放到用户上
        user.assignmentData(adminService.getUserRole(user.getId(), !user.getHasManager(), true)); // 管理员不加载菜单
        ManagerSessionUtil.whenLogin(user, user.getPermissions());
        return JsonResult.success("登录成功并返回用户及菜单信息", ManagerUserRes.assemblyData(user, user.getMenus()));
    }

    @NotNeedPermission
    @ApiMethod(value = "获取用户及菜单信息", index = 1)
    @GetMapping("/info")
    public JsonResult<ManagerUserRes> info() {
        Long userId = ManagerSessionUtil.getUserId();
        ManagerUser user = adminService.getUser(userId);
        user.assignmentData(adminService.getUserRole(userId, true, false));
        return JsonResult.success("获取用户及菜单信息", ManagerUserRes.assemblyData(user, user.getMenus()));
    }

    @NotNeedPermission
    @ApiMethod(value = "修改密码", index = 2)
    @PostMapping("/password")
    public JsonResult<Void> password(String oldPass, String newPass) {
        Long userId = ManagerSessionUtil.getUserId();
        adminService.updatePassword(userId, oldPass, newPass);
        return JsonResult.success("密码修改成功");
    }

    @NotNeedPermission
    @ApiMethod(value = "修改基本信息", index = 2)
    @PostMapping("/basic")
    public JsonResult<Void> update(@ApiParam("昵称") String nickName, @ApiParam("头像") MultipartFile file) {
        Long userId = ManagerSessionUtil.getUserId();
        ManagerUser user = new ManagerUser();
        user.setId(userId);
        user.setNickName(nickName);
        user.setAvatar(FileUtil.save(file, config.getFilePath(), config.getFileUrl(), false));
        adminService.addOrUpdateUser(user);
        return JsonResult.success("信息修改成功");
    }

    @NotNeedLogin
    @ApiMethod("退出")
    @GetMapping("/logout")
    public JsonResult<Void> login() {
        ManagerSessionUtil.signOut();
        return JsonResult.success("退出成功");
    }
}
