package com.github.manager.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 角色, 与 用户是 多对多 的关系 --> t_manager_role */
@Data
@TableName("t_manager_role")
public class ManagerRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 角色名 --> name */
    private String name;


    // 下面的字段不与数据库关联, 只做为数据载体进行传输

    /** 角色下的菜单 id */
    @TableField(exist = false)
    private List<Long> menuIds;

    /** 角色下的菜单 */
    @TableField(exist = false)
    private List<ManagerMenu> menus;

    /** 角色下的权限 id */
    @TableField(exist = false)
    private List<Long> permissionIds;

    /** 角色下的权限 */
    @TableField(exist = false)
    private List<ManagerPermission> permissions;
}
