package com.github.vo;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import com.github.manager.model.ManagerMenu;
import com.github.manager.model.ManagerUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@Data
@Accessors(chain = true)
public class ManagerUserVo {
    @ApiReturn("用户id")
    private Long id;

    @ApiReturn("用户名")
    private String userName;

    @ApiReturn("昵称")
    private String nickName;

    @ApiReturn("头像")
    private String avatar;

    @ApiReturn("true 则表示是管理员")
    private boolean hasAdmin;

    @ApiReturn("用户能见到的菜单")
    private List<ManagerMenuVo> menus;


    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class ManagerMenuVo {
        // @ApiReturn("菜单 id")
        // private Long id;

        @ApiReturn("前端对应的值")
        private String front;

        @ApiReturn("子菜单")
        private List<ManagerMenuVo> children;

        public ManagerMenuVo(String front) {
            this.front = front;
        }
        public ManagerMenuVo(String front, List<ManagerMenuVo> children) {
            this.front = front;
            this.children = children;
        }
    }


    public static ManagerUserVo assemblyData(ManagerUser user, List<ManagerMenu> menus) {
        ManagerUserVo vo = JsonUtil.convert(user, ManagerUserVo.class);
        if (U.isNotBlank(vo)) {
            List<ManagerMenuVo> menuVos = JsonUtil.convertList(menus, ManagerMenuVo.class);
            if (A.isNotEmpty(menuVos)) {
                vo.setMenus(menuVos);
            }
        }
        return vo;
    }

    public static ManagerUserVo testData() {
        ManagerUserVo vo = new ManagerUserVo();
        vo.setId(123L);
        vo.setUserName("zhanshan");
        vo.setNickName("张三");

        List<ManagerMenuVo> menus = Arrays.asList(
                new ManagerMenuVo("common", Arrays.asList(
                        new ManagerMenuVo("config-index"),
                        new ManagerMenuVo("config-add"),
                        new ManagerMenuVo("config-edit"),

                        new ManagerMenuVo("banner-index"),
                        new ManagerMenuVo("banner-add"),
                        new ManagerMenuVo("banner-edit")
                )),
                new ManagerMenuVo("user", Arrays.asList(
                        new ManagerMenuVo("user-index"),
                        new ManagerMenuVo("user-id"),
                        new ManagerMenuVo("user-add"),
                        new ManagerMenuVo("user-edit")
                )),
                new ManagerMenuVo("product", Arrays.asList(
                        new ManagerMenuVo("product-index"),
                        new ManagerMenuVo("product-id"),
                        new ManagerMenuVo("product-add"),
                        new ManagerMenuVo("product-edit")
                )),
                new ManagerMenuVo("order", Arrays.asList(
                        new ManagerMenuVo("order-index"),
                        new ManagerMenuVo("order-id")
                )),
                new ManagerMenuVo("manager", Arrays.asList(
                        new ManagerMenuVo("account-index"),
                        new ManagerMenuVo("account-add"),
                        new ManagerMenuVo("account-edit"),
        
                        new ManagerMenuVo("role-index"),
                        new ManagerMenuVo("role-add"),
                        new ManagerMenuVo("role-edit")
                ))
        );

        vo.setMenus(menus);
        // vo.setHasAdmin(true);
        return vo;
    }
}
