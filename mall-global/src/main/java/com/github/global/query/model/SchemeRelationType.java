package com.github.global.query.model;

public enum SchemeRelationType {

    NULL,

    ONE_TO_ONE,
    ONE_TO_MANY;
    // 不做 Many to Many 的关联: 这种情况下建多一个「中间表」, 由「中间表」跟「目标表」关联成两个多对一来实现
}
