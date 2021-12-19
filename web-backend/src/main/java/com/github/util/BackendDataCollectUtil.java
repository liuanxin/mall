package com.github.util;

import com.github.common.constant.CommonConst;
import com.github.common.resource.CollectEnumUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.global.constant.GlobalConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;
import com.google.common.base.CaseFormat;

import java.util.HashMap;
import java.util.Map;

/** 从各模块中收集数据的工具类 */
@SuppressWarnings("rawtypes")
public final class BackendDataCollectUtil {

    private static final Map<String, Class> ENUM_MAP = A.maps(
            GlobalConst.MODULE_NAME, GlobalConst.class,

            CommonConst.MODULE_NAME, CommonConst.class,
            UserConst.MODULE_NAME, UserConst.class,
            ProductConst.MODULE_NAME, ProductConst.class,
            OrderConst.MODULE_NAME, OrderConst.class
    );

    // /** 放到渲染上下文的枚举数组 */
    // public static final Class[] VIEW_ENUM_ARRAY = CollectEnumUtil.getEnumClass(ENUM_MAP);

    /** 提供接口出去的 所有 枚举信息 */
    public static final Map<String, Map<String, Object>> ALL_ENUM_INFO = CollectEnumUtil.enumMap(ENUM_MAP);
    /** 提供接口出去的 单个 枚举信息 */
    public static Map<String, Map<String, Object>> singleEnumInfo(String type) {
        Map<String, Map<String, Object>> returnMap = new HashMap<>();
        for (String anEnum : type.split(",")) {
            if (U.isNotBlank(anEnum)) {
                anEnum = anEnum.trim();
                String name;
                if (anEnum.contains("-")) {
                    name = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, anEnum);
                } else if (anEnum.contains("_")) {
                    name = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, anEnum);
                } else if (Character.isUpperCase(anEnum.charAt(0))) {
                    name = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, anEnum);
                } else {
                    name = anEnum;
                }
                Map<String, Object> map = ALL_ENUM_INFO.get(name);
                if (A.isNotEmpty(map)) {
                    returnMap.put(anEnum, map);
                }
            }
        }
        return returnMap;
    }
}
