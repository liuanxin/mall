package com.github.common.bean;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CollectProperty {

    /** 属性名. 如: 支付类型 */
    String value();

    /** 当前属性在哪些组上收集 */
    Group[] group() default Group.ALL;

    /**
     * 值映射. 可以转换成 Map&lt;String, String&gt; 的 json 串, 大写的 OTHER 表示其他
     *
     * 如: { "1":"支付宝", "2":"微信", "OTHER":"其他" }, 当标注的属性值是 1 时收集的时候会显示成 支付宝
     */
    String valueMapping() default "";

    /** 如果被标注的属性是时间类型时的输出格式  */
    String dateFormat() default "yyyy-MM-dd HH:mm:ss";

    /** 排序, 越小越靠前 */
    int order() default Integer.MAX_VALUE;

    enum Group {
        ALL, CREATE, UPDATE, DELETE;
    }
}
