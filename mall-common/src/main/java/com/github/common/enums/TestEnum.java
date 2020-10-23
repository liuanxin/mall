package com.github.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TestEnum {

    One(1), Two(2), Three(3);

    @EnumValue
    int code;
    TestEnum(int code) { this.code = code; }

    public int getCode() { return code; }
}
