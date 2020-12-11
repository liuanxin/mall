package com.github.user.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.user.enums.UserTestLevel;
import lombok.Data;

import java.io.Serializable;

/** 用户 --> t_user_test */
@Data
@TableName("t_user_test")
public class UserTest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户名 --> user_name */
    private String userName;

    /** 密码 --> password */
    private String password;

    /** 用户等级(0.未知, 1.普通, 2.vip) --> level */
    private UserTestLevel level;


    // 下面的属性不与数据库关联字段

    @TableField(exist = false)
    private UserTestExtend extend;
}
