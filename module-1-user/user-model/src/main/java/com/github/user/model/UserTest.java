package com.github.user.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

/** 用户 --> t_user_test */
@Data
@Table("t_user_test")
public class UserTest {

    private Long id;

    /** 用户名 --> user_name */
    private String userName;

    /** 密码 --> password */
    private String password;

    /** 用户等级(0.未知, 1.普通, 2.vip) --> level */
    private Integer level;


    // 下面的属性不与数据库关联字段

    @Column(ignore = true)
    private UserTestExtend extend;
}
