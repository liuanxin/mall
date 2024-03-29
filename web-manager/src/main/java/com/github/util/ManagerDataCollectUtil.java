package com.github.util;

import com.github.common.constant.CommonConst;
import com.github.common.util.A;
import com.github.common.util.CollectClassUtil;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import com.github.manager.constant.ManagerConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;

import java.util.HashMap;
import java.util.Map;

/** 从各模块中收集数据的工具类 */
@SuppressWarnings("rawtypes")
public final class ManagerDataCollectUtil {

    private static final Map<String, Class> ENUM_MAP = A.maps(
            GlobalConst.MODULE_NAME, GlobalConst.class,

            CommonConst.MODULE_NAME, CommonConst.class,
            UserConst.MODULE_NAME, UserConst.class,
            ProductConst.MODULE_NAME, ProductConst.class,
            OrderConst.MODULE_NAME, OrderConst.class,
            ManagerConst.MODULE_NAME, ManagerConst.class
    );

    // /** 放到渲染上下文的枚举数组 */
    // public static final Class[] VIEW_ENUM_ARRAY = CollectEnumUtil.getEnumClass(ENUM_MAP);

    /** 提供接口出去的 所有 枚举信息 */
    public static final Map<String, Object> ALL_ENUM_INFO = CollectClassUtil.getEnumMap(ENUM_MAP);
    /** 提供接口出去的 单个 枚举信息 */
    public static Map<String, Object> singleEnumInfo(String type) {
        Map<String, Object> returnMap = new HashMap<>();
        for (String anEnum : type.split(",")) {
            if (U.isNotBlank(anEnum)) {
                Object data = ALL_ENUM_INFO.get(anEnum.trim());
                if (U.isNotNull(data)) {
                    returnMap.put(anEnum, data);
                }
            }
        }
        return returnMap;
    }
}
