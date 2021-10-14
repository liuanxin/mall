package com.github.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TestEnum {

    NIL(0), One(1), Two(2), Three(3);

    @EnumValue
    private final int code;
}
