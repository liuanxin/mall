package com.github.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TestEnum {

    NIL(0), One(1), Two(2), Three(3);

    @EnumValue
    private final int code;
}
