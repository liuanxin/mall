package com.github.common.export.poi;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExportColumn {

    /** 字段说明 */
    String value();
}
