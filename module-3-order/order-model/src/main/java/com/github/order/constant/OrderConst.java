package com.github.order.constant;

import com.github.common.Const;

/**
 * 订单模块相关的常数设置类
 */
public final class OrderConst {

    /** 当前模块名 */
    public static final String MODULE_NAME = "order";

    /** 当前模块说明 */
    public static final String MODULE_INFO = MODULE_NAME + "-订单";

    /** mybatis 扫描当前模块的目录 */
    public static final String SCAN = Const.BASE_PACKAGE + "." + MODULE_NAME + ".repository";
}
