package com.github.global.query.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableInfo {

    /** table name */
    String value();

    /** table comment */
    String desc() default "";

    /** table alias, use table name if empty */
    String alias() default "";

    /** true: this class is not associated with a table */
    boolean ignore() default false;
}
