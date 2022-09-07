package com.github.global.query.annotation;

import java.lang.annotation.*;

/** 标注在类上, 用来表示跟数据库表的对应关系 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SchemaInfo {

    /** 数据库表名 */
    String value();

    /** 数据库表说明 */
    String desc() default "";

    /** 表别名, 为空时则使用类名 */
    String alias() default "";

    /** true 表示这个类不与表有关联 */
    boolean ignore() default false;
}
