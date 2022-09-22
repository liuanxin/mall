package com.github.global.query.enums;

public enum TableRelationType {

    NULL,

    ONE_TO_ONE,
    ONE_TO_MANY,
    // MANY_TO_MANY
    ;

    public boolean hasMany() {
        return this == ONE_TO_MANY; // || this == MANY_TO_MANY;
    }
}
