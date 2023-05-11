package com.github.manager.model;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 角色 和 权限 的 中间表 --> t_manager_role_permission */
@Data
@Accessors(chain = true)
@Table("t_manager_role_permission")
public class ManagerRolePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 角色 id --> role_id */
    private Long roleId;

    /** 权限 id --> permissionId */
    private Long permissionId;
}
