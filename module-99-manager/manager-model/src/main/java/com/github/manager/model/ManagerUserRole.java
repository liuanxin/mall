package com.github.manager.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/** 用户 和 角色 的中间表 --> t_manager_user_role */
@Data
@Accessors(chain = true)
@TableName("t_manager_user_role")
public class ManagerUserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户 id --> user_id */
    private Long userId;

    /** 角色 id --> role_id */
    private Long roleId;
}
