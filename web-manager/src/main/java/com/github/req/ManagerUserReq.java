package com.github.req;

import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.manager.model.ManagerUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
public class ManagerUserReq {

    @ApiParam("添加时不需要")
    private Long id;

    @ApiParam("角色名")
    private String userName;

    @ApiParam("昵称")
    private String nickName;

    @ApiParam("管理员要重置密码, 只传入 id 和密码即可")
    private String password;

    @ApiParam("0 表示正常, 1 表示已禁用, 管理员要修改用户状态, 只传入 id 和 状态即可")
    private Boolean status;

    @ApiParam("用户的角色 id, 多个用逗号分隔")
    private List<Long> rids;

    public boolean hasUpdate() {
        return U.greater0(id);
    }
    public void basicCheck() {
        if (U.isNotBlank(userName)) {
            int max = 20;
            U.assertException(userName.trim().length() > max, String.format("用户名请保持在 %s 个字以内", max));
        }
        if (U.isNotBlank(password)) {
            int max = 16;
            U.assertException(password.length() > max, String.format("密码长度只需要在 %s 位即可", max));
        }
    }

    public ManagerUser operateParam() {
        return JsonUtil.convert(this, ManagerUser.class);
    }
}
