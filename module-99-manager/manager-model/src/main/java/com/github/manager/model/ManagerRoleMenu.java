package com.github.manager.model;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 角色 和 菜单 的中间表 --> t_manager_role_menu */
@Data
@Accessors(chain = true)
@Table("t_manager_role_menu")
public class ManagerRoleMenu implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 角色 id --> role_id */
    private Long roleId;

    /** 菜单 id --> menu_id */
    private Long menuId;
}
