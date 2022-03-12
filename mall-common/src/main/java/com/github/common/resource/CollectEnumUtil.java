package com.github.common.resource;

import com.github.common.Const;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.base.CaseFormat;

import java.util.*;

@SuppressWarnings("rawtypes")
public final class CollectEnumUtil {

    /** 如果需要在 enum 中返回给前台的下拉数据, 添加一个这样的方法名, 返回结果用 Map. 如果有定义将无视下面的 code 和 value */
    private static final String METHOD = "select";

    private static final String CODE = "code";
    /** 在 enum 中返回给前台的下拉数据的键, 如果没有将会以 ordinal() 为键 */
    private static final String CODE_METHOD = "getCode";

    private static final String VALUE = "value";
    /** 在 enum 中返回给前台的下拉数据的值, 如果没有将会以 name() 为值 */
    private static final String VALUE_METHOD = "getValue";

    /**
     * 从 指定模块指定类 获取所有的枚举类(放入 view 的上下文).
     *
     * key 表示模块名(包含在枚举所在类的包名上), value 表示枚举类所在包的类(用来获取 ClassLoader)
     */
    public static Class[] getEnumClass(Map<String, Class> enumMap) {
        Set<Class> set = new HashSet<>();
        for (Map.Entry<String, Class> entry : enumMap.entrySet()) {
            List<Class> enums = LoaderClass.getEnumArray(entry.getValue(), Const.enumPath(entry.getKey()));
            for (Class anEnum : enums) {
                if (U.isNotNull(anEnum) && anEnum.isEnum()) {
                    // 将每个模块里面的枚举都收集起来, 然后会放入到渲染上下文里面去
                    set.add(anEnum);
                }
            }
        }
        return set.toArray(new Class[0]);
    }

    /** 获取所有枚举的说明 */
    @SuppressWarnings("rawtypes")
    public static Map<String, Object> enumMap(Map<String, Class> enumClassMap) {
        Map<String, Object> returnMap = new HashMap<>();
        for (Map.Entry<String, Class> entry : enumClassMap.entrySet()) {
            List<Class> enumList = LoaderClass.getEnumArray(entry.getValue(), Const.enumPath(entry.getKey()));
            for (Class anEnum : enumList) {
                if (U.isNotNull(anEnum) && anEnum.isEnum()) {
                    returnMap.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, anEnum.getSimpleName()), enumInfo(anEnum));
                }
            }
        }
        return returnMap;
    }

    /** 根据枚举的名字获取单个枚举的说明. loadEnum 为 true 表示需要基于加载器去获取相关包里面的 枚举 */
    @SuppressWarnings({"rawtypes"})
    private static Object enumInfo(Class<?> enumClass) {
        if (!enumClass.isEnum()) {
            return null;
        }
        try {
            // 在 enum 中如果有静态的 select 方法且返回的是 Map 就用这个
            Object result = enumClass.getMethod(METHOD).invoke(null);
            if (U.isNotNull(result)) {
                return result;
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("call ({}) method({}) exception", enumClass, METHOD, e);
            }
        }

        List<Map<String, Object>> returnList = new ArrayList<>();
        for (Object anEnum : enumClass.getEnumConstants()) {
            // 没有 getCode 方法就使用枚举的 ordinal
            Object key = U.invokeMethod(anEnum, CODE_METHOD);
            if (key == null) {
                key = ((Enum) anEnum).ordinal();
            }

            // 没有 getValue 方法就使用枚举的 name
            Object value = U.invokeMethod(anEnum, VALUE_METHOD);
            if (value == null) {
                value = ((Enum) anEnum).name();
            }

            returnList.add(Map.of(CODE, U.toStr(key), VALUE, value));
        }
        return returnList;
    }
}
