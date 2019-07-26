package com.github.manager.constant;

import com.github.common.Const;

/**
 * 管理模块相关的常数设置类
 */
public final class ManagerConst {

    /** 当前模块名 */
    public static final String MODULE_NAME = "manager";

    /** 当前模块说明 */
    public static final String MODULE_INFO = MODULE_NAME + "-管理";

    /** mybatis 扫描当前模块的目录 */
    public static final String SCAN = Const.BASE_PACKAGE + "." + MODULE_NAME + ".repository";
}
