package com.github.common.util;

import com.github.common.Const;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings("rawtypes")
public final class CollectClassUtil {

    /** 如果需要在 enum 中返回给前台的下拉数据, 添加一个这样的方法名, 返回结果用 Map. 如果有定义将无视下面的 code 和 value */
    private static final String METHOD = "select";

    private static final String CODE = "code";
    /** 在 enum 中返回给前台的下拉数据的键, 如果没有将会以 ordinal() 为键 */
    private static final String CODE_METHOD = "getCode";

    private static final String VALUE = "value";
    /** 在 enum 中返回给前台的下拉数据的值, 如果没有将会以 name() 为值 */
    private static final String VALUE_METHOD = "getValue";

    /**
     * 获取所有枚举的说明
     *
     * @param enumClassMap key 表示模块名(包含在枚举所在类的包名上), value 表示枚举类所在包的类(用来获取 ClassLoader)
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map<String, Object> getEnumMap(Map<String, Class> enumClassMap) {
        Map<String, Object> returnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Class> entry : enumClassMap.entrySet()) {
            List<Class<?>> enumList = getEnumListInPackage(entry.getValue(), Const.enumPath(entry.getKey()));
            for (Class anEnum : enumList) {
                if (U.isNotNull(anEnum) && anEnum.isEnum()) {
                    String key = anEnum.getSimpleName();
                    try {
                        // 在 enum 中如果有静态的 select 方法且返回的是 Map 就用这个
                        Object result = anEnum.getMethod(METHOD).invoke(null);
                        if (U.isNotNull(result)) {
                            returnMap.put(key, result);
                            break;
                        }
                    } catch (Exception ignore) {
                    }

                    List<Map<String, Object>> returnList = new ArrayList<>();
                    for (Object en : anEnum.getEnumConstants()) {
                        // 没有 getCode 方法就使用枚举的 ordinal
                        Object k = U.invokeMethod(en, CODE_METHOD);
                        if (U.isNull(k)) {
                            k = ((Enum) en).ordinal();
                        }

                        // 没有 getValue 方法就使用枚举的 name
                        Object value = U.invokeMethod(en, VALUE_METHOD);
                        if (U.isNull(value)) {
                            value = ((Enum) en).name();
                        }
                        returnList.add(Map.of(CODE, U.toStr(k), VALUE, value));
                    }

                    returnMap.put(key, returnList);
                }
            }
        }
        return returnMap;
    }


    /** 基于指定的类(会基于此类来获取类加载器)在指定的包名下获取所有的枚举类 */
    public static List<Class<?>> getEnumListInPackage(Class<?> clazz, String classPackage) {
        return getClassList(clazz, classPackage, true);
    }

    /** 基于指定的类(会基于此类来获取类加载器)在指定的包名下获取所有的非枚举类 */
    public static List<Class<?>> getClassListInPackage(Class<?> clazz, String classPackage) {
        return getClassList(clazz, classPackage, false);
    }

    private static List<Class<?>> getClassList(Class<?> clazz, String classPackage, boolean wasEnum) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("{} in ({})", clazz, U.getClassInFile(clazz));
        }
        List<Class<?>> classList = new ArrayList<>();
        String packageName = classPackage.replace(".", "/");
        URL url = clazz.getClassLoader().getResource(packageName);
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                File parent = new File(url.getPath());
                if (parent.isDirectory()) {
                    File[] files = parent.listFiles();
                    if (A.isNotEmpty(files)) {
                        for (File file : files) {
                            Class<?> aClass;
                            if (wasEnum) {
                                aClass = getEnum(file.getName(), classPackage);
                            } else {
                                aClass = getClass(file.getName(), classPackage);
                            }
                            if (aClass != null) {
                                classList.add(aClass);
                            }
                        }
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
                try (JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(packageName) && name.endsWith(".class")) {
                            Class<?> aClass;
                            if (wasEnum) {
                                aClass = getEnum(name.substring(name.lastIndexOf("/") + 1), classPackage);
                            } else {
                                aClass = getClass(name.substring(name.lastIndexOf("/") + 1), classPackage);
                            }
                            if (aClass != null) {
                                classList.add(aClass);
                            }
                        }
                    }
                } catch (IOException e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("can't load jar file", e);
                    }
                }
            }
        }
        return classList;
    }

    private static Class<?> getEnum(String name, String classPackage) {
        if (U.isNotBlank(name)) {
            String className = classPackage + "." + name.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isEnum()) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("can't load class file ({})", className, e);
                }
            }
        }
        return null;
    }

    private static Class<?> getClass(String name, String classPackage) {
        if (U.isNotBlank(name)) {
            String className = classPackage + "." + name.replace(".class", "");
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("can't load class file({})", className, e);
                }
            }
        }
        return null;
    }
}
