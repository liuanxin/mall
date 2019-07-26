package com.github.config;

import com.github.common.config.CommonConfigData;
import com.github.common.resource.CollectTypeHandlerUtil;
import com.github.common.util.A;
import com.github.global.constant.GlobalConst;
import com.github.order.config.OrderConfigData;
import com.github.product.config.ProductConfigData;
import com.github.user.config.UserConfigData;
import com.google.common.collect.Lists;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.List;

/**
 * 管理模块的配置数据. 主要是 mybatis 的多配置目录和类型处理器
 */
final class BackendConfigData {

    /** 要加载的 mybatis 的配置文件目录 */
    static final Resource[] RESOURCE_ARRAY;

    /** 要加载的 mybatis 类型处理器的目录 */
    static final TypeHandler[] HANDLER_ARRAY;

    static {
        List<Resource> resources = Lists.newArrayList();
        Collections.addAll(resources, CommonConfigData.RESOURCE_ARRAY);
        Collections.addAll(resources, UserConfigData.RESOURCE_ARRAY);
        Collections.addAll(resources, ProductConfigData.RESOURCE_ARRAY);
        Collections.addAll(resources, OrderConfigData.RESOURCE_ARRAY);
        RESOURCE_ARRAY = resources.toArray(new Resource[resources.size()]);

        List<TypeHandler> typeHandlers = Lists.newArrayList();
        Collections.addAll(typeHandlers, CollectTypeHandlerUtil.typeHandler(A.maps(GlobalConst.MODULE_NAME, GlobalConst.class)));
        Collections.addAll(typeHandlers, CommonConfigData.HANDLER_ARRAY);
        Collections.addAll(typeHandlers, UserConfigData.HANDLER_ARRAY);
        Collections.addAll(typeHandlers, ProductConfigData.HANDLER_ARRAY);
        Collections.addAll(typeHandlers, OrderConfigData.HANDLER_ARRAY);
        HANDLER_ARRAY = typeHandlers.toArray(new TypeHandler[typeHandlers.size()]);
    }
}
