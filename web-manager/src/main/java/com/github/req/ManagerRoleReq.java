package com.github.req;

import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.manager.model.ManagerRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
public class ManagerRoleReq {

    @ApiParam("角色 id, 更新时需要, 添加不需要")
    private Long id;

    @ApiParam("角色名")
    private String name;

    @ApiParam("菜单 id, 多个用逗号隔开")
    private List<Long> mids;

    @ApiParam("权限 id, 多个用逗号隔开")
    private List<Long> pids;

    public boolean hasUpdate() {
        return U.greater0(id);
    }
    public void basicCheck() {
        int max = 20;
        U.assertException(U.isBlank(name) || name.trim().length() > max, String.format("角色名要有, 且在 %s 个字以内", max));
    }

    public ManagerRole operateParam() {
        return JsonUtil.convert(this, ManagerRole.class);
    }
}
