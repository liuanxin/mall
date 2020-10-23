package com.github.user.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.common.enums.Gender;
import com.github.user.enums.UserTestLevel;
import lombok.Data;

import java.io.Serializable;

/** 用户示例表 --> t_user_test */
@Data
@TableName("t_user_test")
public class UserTest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 昵称 --> nick_name */
    private String nickName;

    /** 性别 --> gender */
    private Gender gender;

    /** 等级 --> level */
    private UserTestLevel level;

    /** 头像 --> avatar_url */
    private String avatarUrl;
}
