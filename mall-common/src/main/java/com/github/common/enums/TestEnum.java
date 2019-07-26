package com.github.common.enums;

public enum TestEnum {

    One(1), Two(2), Three(3);

    int code;
    TestEnum(int code) { this.code = code; }

    public int getCode() { return code; }
}
