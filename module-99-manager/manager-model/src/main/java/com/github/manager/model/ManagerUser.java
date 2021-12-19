package com.github.manager.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.common.util.A;
import com.google.common.collect.Sets;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 用户 --> t_manager_user */
@Data
@TableName("t_manager_user")
public class ManagerUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户名 --> user_name */
    private String userName;

    /** 密码 --> password */
    private String password;

    /** 昵称 --> nick_name */
    private String nickName;

    /** 头像 --> avatar */
    private String avatar;

    /** 1 表示是管理员 --> has_manager */
    private Boolean hasManager;

    /** 1 表示已禁用 --> status */
    private Boolean status;


    // 下面的字段不与数据库关联, 只做为数据载体进行传输

    /** 用户的角色 id, 添加修改时用到 */
    @TableField(exist = false)
    private List<Long> roleIds;

    /** 用户的所有角色下的所有菜单 */
    @TableField(exist = false)
    private List<ManagerMenu> menus;

    /** 用户的所有角色下的所有权限 */
    @TableField(exist = false)
    private List<ManagerPermission> permissions;

    /** 一个用户有多个角色, 一个角色又有多个菜单(且会有父子层级关系)和多个权限, 基于用户的角色将菜单和权限赋值 */
    public void assignmentData(List<ManagerRole> roles) {
        if (A.isNotEmpty(roles)) {
            List<ManagerMenu> menuList = new ArrayList<>();
            Set<ManagerPermission> set = Sets.newHashSet();
            for (ManagerRole role : roles) {
                menuList.addAll(role.getMenus());
                set.addAll(role.getPermissions()); // 权限去重
            }
            this.menus = ManagerMenu.handleAllMenu(menuList);
            this.permissions = new ArrayList<>(set);
        }
    }
}
