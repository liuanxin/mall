package com.github.vo;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import com.github.manager.model.ManagerMenu;
import com.github.manager.model.ManagerUser;
import com.google.common.collect.Lists;
import lombok.Data;
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
    @Accessors(chain = true)
    public static class ManagerMenuVo {
        // @ApiReturn("菜单 id")
        // private Long id;

        @ApiReturn("菜单说明")
        private String name;

        @ApiReturn("前端对应的值")
        private String front;

        @ApiReturn("子菜单")
        private List<ManagerMenuVo> children;
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

        List<ManagerMenuVo> menus = Lists.newArrayList();

        menus.add(new ManagerMenuVo().setName("公共管理").setFront("common").setChildren(Arrays.asList(
                new ManagerMenuVo().setName("全局配置").setFront("config-index"),
                new ManagerMenuVo().setName("添加全局配置").setFront("config-add"),
                new ManagerMenuVo().setName("编辑全局配置").setFront("config-edit"),

                new ManagerMenuVo().setName("banner").setFront("banner-index"),
                new ManagerMenuVo().setName("添加 banner").setFront("banner-add"),
                new ManagerMenuVo().setName("编辑 banner").setFront("banner-edit")
        )));
        menus.add(new ManagerMenuVo().setName("用户管理").setFront("user").setChildren(Arrays.asList(
                new ManagerMenuVo().setName("用户列表").setFront("user-index"),
                new ManagerMenuVo().setName("用户详情").setFront("user-id"),
                new ManagerMenuVo().setName("添加用户").setFront("user-add"),
                new ManagerMenuVo().setName("编辑用户").setFront("user-edit")
        )));
        menus.add(new ManagerMenuVo().setName("商品管理").setFront("product").setChildren(Arrays.asList(
                new ManagerMenuVo().setName("商品列表").setFront("product-index"),
                new ManagerMenuVo().setName("商品详情").setFront("product-id"),
                new ManagerMenuVo().setName("添加商品").setFront("product-add"),
                new ManagerMenuVo().setName("编辑商品").setFront("product-edit")
        )));
        menus.add(new ManagerMenuVo().setName("订单管理").setFront("order").setChildren(Arrays.asList(
                new ManagerMenuVo().setName("订单列表").setFront("order-index"),
                new ManagerMenuVo().setName("订单详情").setFront("order-id")
        )));
        menus.add(new ManagerMenuVo().setName("系统管理").setFront("manager").setChildren(Arrays.asList(
                new ManagerMenuVo().setName("人员列表").setFront("account-index"),
                new ManagerMenuVo().setName("添加人员").setFront("account-add"),
                new ManagerMenuVo().setName("编辑人员").setFront("account-edit"),

                new ManagerMenuVo().setName("角色列表").setFront("role-index"),
                new ManagerMenuVo().setName("添加角色").setFront("role-add"),
                new ManagerMenuVo().setName("编辑角色").setFront("role-edit")
        )));

        vo.setMenus(menus);
        // vo.setHasAdmin(true);
        return vo;
    }
}
