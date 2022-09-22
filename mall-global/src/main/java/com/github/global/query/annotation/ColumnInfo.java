package com.github.global.query.annotation;

import com.github.global.query.enums.TableRelationType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ColumnInfo {

    /** table column name */
    String value();

    /** table column comment */
    String desc() default "";

    /** table column alias, use column name if empty */
    String alias() default "";

    /** true: this field is not associated with a column */
    boolean ignore() default false;

    /** true: this column is primary key */
    boolean primary() default false;

    int varcharLength() default 0;

    /** 关联类型, 设置了 relationTable 和 relationColumn 才有效, 只在「从表」对应的类上标, 主表上无需标注 */
    TableRelationType relationType() default TableRelationType.NULL;

    /** 设置了 relationType 非 NULL 时有效, 关联表的列名, 配合 relationType 一起使用, 只在「从表」对应的类上标, 主表上无需标注 */
    String relationTable() default "";

    /** 设置了 relationType 非 NULL 时有效, 关联表的列名, 配合 relationTable 一起使用, 只在「从表」对应的类上标, 主表上无需标注 */
    String relationColumn() default "";
}
