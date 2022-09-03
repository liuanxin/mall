package com.github.global.query.annotation;

import java.lang.annotation.*;

/** 标注在类上, 用来表示跟数据库表的对应关系 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** 表列名, 为空则自动基于 类字段名 来(类字段 userName --> 表列 user_name) */
    String value() default "";

    /** 表列说明 */
    String desc() default "";

    /** 表列别名, 为空时则使用类字段名 */
    String alias() default "";

    /** true 表示这个字段不与列名关联 */
    boolean ignore() default false;

    /** true 表示是主键字段 */
    boolean primary() default false;

    /** 一对多关联的表, 配合 relationColumn 一起使用, 只在从表的上标即可, 主表上无需标注 */
    SchemeRelationType relationType() default SchemeRelationType.NULL;

    /** 关联表的列名, 配合 relationType 一起使用, 只在「从表是多」的类上标即可, 主表上无需标注 */
    String relationColumn() default "";

    enum SchemeRelationType {
        NULL,

        ONE_TO_ONE,
        ONE_TO_MANY;
        // 不做 Many to Many 的关联: 这种情况下建多一个「中间表」, 由「中间表」跟「目标表」关联成两个多对一来实现
    }
}
