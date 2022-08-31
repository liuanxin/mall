package com.github.global.query.annotation;

import java.lang.annotation.*;

/** 标注在类上, 用来表示跟数据库表的对应关系 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableColumnInfo {

    /** 表列名, 为空则自动基于 类字段名 来(类字段 userName --> 表列 user_name) */
    String value() default "";

    /** 表列说明 */
    String desc() default "";

    /** 表列别名, 为空时则使用类字段名 */
    String alias() default "";

    /** true 表示这个 类字段 不与 表列名 有关联 */
    boolean ignore() default false;

    /** 一对一关联的表, 配合 toOneColumn 一起使用, 只在从表的类上标, 主表上无需标注 */
    String toOneTable() default "";
    /** 一对一关联的表里的列名, 配合 toOneTable 一起使用, 只在从表的类上标注, 主表上无需标注 */
    String toOneColumn() default "";

    /** 一对多关联的表, 配合 toManyColumn 一起使用, 只在「从表是多」的类上上标即可, 主表上无需标注 */
    String toManyTable() default "";
    /** 一对多关联的表里的列名, 配合 toManyTable 一起使用, 只在「从表是多」的类上标即可, 主表上无需标注 */
    String toManyColumn() default "";

    // 不做 Many to Many 的关联: 这种情况下建多一个「中间表」, 由「中间表」跟「目标表」关联成两个多对一来实现
}
