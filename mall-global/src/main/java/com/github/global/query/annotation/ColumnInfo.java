package com.github.global.query.annotation;

import com.github.global.query.enums.TableRelationType;

import java.lang.annotation.*;

/** 标注在类上, 用来表示跟数据库表的对应关系 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** 表列名 */
    String value();

    /** 表列说明 */
    String desc() default "";

    /** 表列别名, 为空时则使用类字段名 */
    String alias() default "";

    /** true 表示这个字段不与列名关联 */
    boolean ignore() default false;

    /** true 表示是主键字段 */
    boolean primary() default false;

    /** 字符串长度 */
    int varcharLength() default 0;

    /** 关联类型, 设置了 relationTable 和 relationColumn 才有效, 只在「从表」对应的类上标, 主表上无需标注 */
    TableRelationType relationType() default TableRelationType.NULL;

    /** 设置了 relationType 非 NULL 时有效, 关联表的列名, 配合 relationType 一起使用, 只在「从表」对应的类上标, 主表上无需标注 */
    String relationTable() default "";

    /** 设置了 relationType 非 NULL 时有效, 关联表的列名, 配合 relationTable 一起使用, 只在「从表」对应的类上标, 主表上无需标注 */
    String relationColumn() default "";
}
