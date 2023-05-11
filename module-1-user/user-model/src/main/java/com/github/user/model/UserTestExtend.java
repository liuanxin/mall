package com.github.user.model;

import com.github.common.enums.Gender;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serializable;

/** 用户扩展 --> t_user_test_extend */
@Data
@Table("t_user_test_extend")
public class UserTestExtend implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 用户 id --> user_id */
    private Long userId;

    /** 用户昵称 --> nick_name */
    private String nickName;

    /** 性别(0.未知, 1.男, 2.女) --> gender */
    private Gender gender;

    /** 用户生日(0102 表示 1月2日) --> birthday */
    private String birthday;
}
