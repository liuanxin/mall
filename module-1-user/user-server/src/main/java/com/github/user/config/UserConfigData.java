package com.github.user.config;

import com.github.common.resource.CollectResourceUtil;
import com.github.common.resource.CollectTypeHandlerUtil;
import com.github.common.util.A;
import com.github.user.constant.UserConst;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.io.Resource;

/**
 * 用户模块的配置数据. 主要是 mybatis 的多配置目录和类型处理器
 */
public final class UserConfigData {

    public static final String[] RESOURCE_PATH = new String[] {
            UserConst.MODULE_NAME + "/*.xml",
            UserConst.MODULE_NAME + "-custom/*.xml"
    };
    /** 要加载的 mybatis 的配置文件目录 */
    public static final Resource[] RESOURCE_ARRAY = CollectResourceUtil.resource(A.maps(
            UserConfigData.class, RESOURCE_PATH
    ));

    /** 要加载的 mybatis 类型处理器的目录 */
    public static final TypeHandler[] HANDLER_ARRAY = CollectTypeHandlerUtil.typeHandler(A.maps(
            UserConst.MODULE_NAME, UserConfigData.class
    ));
}
