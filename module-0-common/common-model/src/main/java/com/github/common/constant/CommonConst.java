package com.github.common.constant;

import com.github.common.Const;

/**
 * 公共模块相关的常数设置类
 */
public final class CommonConst {

    /** 当前模块名 */
    public static final String MODULE_NAME = "common";

    /** 当前模块说明 */
    public static final String MODULE_INFO = MODULE_NAME + "-公共";

    /** mybatis 扫描当前模块的目录 */
    public static final String SCAN = Const.BASE_PACKAGE + "." + MODULE_NAME + ".repository";
}
