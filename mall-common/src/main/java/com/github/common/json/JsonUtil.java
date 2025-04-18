package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.common.date.FormatType;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonUtil {

    /*
    使用 JacksonXml 可以像 json 一样使用相关的 api
    ObjectMapper RENDER = new XmlMapper();

    // object to xml
    String xml = RENDER.writeValueAsString(obj);

    // xml to object
    Parent<Child> obj = RENDER.readValue(xml, new TypeReference<Parent<Child>>() {});

    但是需要引入一个包.  !!注意, xml 的适配在 json 前面, 这会导致所有的接口都返回成 xml 格式!!
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
    </dependency>
    */
    private static final Map<String, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    // 默认时间格式. 要自定义在字段上标 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 即可
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(FormatType.YYYY_MM_DD_HH_MM_SS.getValue());
    static {
        DEFAULT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }
    private static final ObjectMapper OBJECT_MAPPER = globalConfig(
            JsonMapper.builder()
                    // 「null、空字符串、list 及 map 为空或长度为 0」不序列化
                    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
                    // 「null、空字符串、list 及 map 为空或长度为 0、数字为 0、时间的毫秒数为 0」不序列化, 最大程度的节省带宽
                    // .serializationInclusion(JsonInclude.Include.NON_DEFAULT)
                    .build()
                    .setDateFormat(DEFAULT_DATE_FORMAT)
    );


    private static final ObjectMapper EMPTY_OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();
    // 上面的使用实体上的注解, 下面不使用, 用在某些场景时分别用到这两个 mapper 来完成一些数据转换
    private static final ObjectMapper IGNORE_OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .configure(MapperFeature.USE_ANNOTATIONS, false)
            .build();

    @SuppressWarnings("deprecation")
    public static ObjectMapper globalConfig(ObjectMapper objectMapper) {
        return objectMapper
                // 反序列化时, 对 json5 部分功能的支持
                .enable(
                        // 支持注释: { /* xx */ "a": "x" } 可以正常解析
                        JsonParser.Feature.ALLOW_COMMENTS,
                        // 支持尾逗号: [ 1, 2, ] 和 { "a": 1, } 可以正常解析
                        JsonParser.Feature.ALLOW_TRAILING_COMMA,
                        // 字符串可以使用单引号: { 'a': 'x' } 可以正常解析
                        JsonParser.Feature.ALLOW_SINGLE_QUOTES,
                        // key 可以不带引号: { a: "x" } 可以正常解析
                        JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
                        // 无穷 NaN 可以引用成数字
                        // JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS,
                        // 反斜线 \ 可以换行
                        // JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
                        // 支持 .123 这样的数字, 123. 不支持
                        // JsonParser.Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS,
                )
                .disable(
                        // 序列化时: date 不要用时间戳
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        // 序列化时: duration(时间量) 不要用时间戳
                        SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS
                )
                .enable(
                        // 反序列化时: 不确定值的枚举返回 null
                        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                        // 反序列化时: 浮点数用 BigDecimal
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
                        // 反序列化时: 整数用 BigInteger
                        // DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
                )
                .disable(
                        // 反序列化时: 不确定的属性不要抛 JsonMappingException
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                // 序列化及反序列化模块
                .registerModule(JsonModule.GLOBAL_MODULE);
    }


    /** 对象转换, 失败将会返回 null */
    public static <S,T> T convert(S source, Class<T> clazz) {
        return convert(source, clazz, false, false);
    }
    /** 集合转换, 失败将会返回空集合 */
    public static <S,T> List<T> convertList(Collection<S> sourceList, Class<T> clazz) {
        return convertList(sourceList, clazz, false, false);
    }
    /** 键值对转换, 失败将会返回空键值对 */
    public static <SK,SV,K,V> Map<K,V> convertMap(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass) {
        return convertMap(sourceMap, keyClass, valueClass, false, false);
    }
    /** 对象转换, 失败将会返回 null */
    public static <T,S> T convertType(S source, TypeReference<T> type) {
        return convertType(source, type, false, false);
    }

    /**
     * 对象转换, 失败将会返回 null
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @Json... 注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @Json... 注解
     */
    public static <S,T> T convert(S source, Class<T> clazz, boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        // noinspection DuplicatedCode
        if (U.isNull(source)) {
            return null;
        }

        String json;
        if (source instanceof String s) {
            json = s;
        } else {
            try {
                json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(source);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("obj({}) to json exception", U.compress(source.toString()), e);
                }
                return null;
            }
        }

        if (U.isBlank(json)) {
            return null;
        }
        try {
            return (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), clazz.getName(), e);
            }
            return null;
        }
    }
    /**
     * 集合转换, 失败将会返回空集合
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @JsonProperty 等注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @JsonProperty 等注解
     */
    public static <S,T> List<T> convertList(Collection<S> sourceList, Class<T> clazz,
                                            boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        if (A.isEmpty(sourceList)) {
            return Collections.emptyList();
        }

        String json;
        try {
            json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(sourceList);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("List({}) to json exception", U.compress(sourceList.toString()), e);
            }
            return Collections.emptyList();
        }
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            ObjectMapper mapper = (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER);
            return mapper.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> mapper.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyList();
        }
    }
    /**
     * 键值对转换, 失败将会返回空键值对
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @JsonProperty 等注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @JsonProperty 等注解
     */
    public static <SK,SV,K,V> Map<K,V> convertMap(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass,
                                                  boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        if (A.isEmpty(sourceMap)) {
            return Collections.emptyMap();
        }

        String json;
        try {
            json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(sourceMap);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Map({}) to json exception", U.compress(sourceMap.toString()), e);
            }
            return Collections.emptyMap();
        }
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            ObjectMapper mapper = (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER);
            return mapper.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> mapper.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyMap();
        }
    }

    /** 对象转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <S,T> T convertIgnoreAnnotation(S source, Class<T> clazz) {
        return convert(source, clazz, true, true);
    }
    /** 集合转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回空集合 */
    public static <S,T> List<T> convertListIgnoreAnnotation(Collection<S> sourceList, Class<T> clazz) {
        return convertList(sourceList, clazz, true, true);
    }
    /** 键值对转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回空键值对 */
    public static <SK,SV,K,V> Map<K,V> convertMapIgnoreAnnotation(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass) {
        return convertMap(sourceMap, keyClass, valueClass, true, true);
    }
    /** 对象转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <T,S> T convertTypeIgnoreAnnotation(S source, TypeReference<T> type) {
        return convertType(source, type, true, true);
    }
    /**
     * 对象转换, 失败将会返回 null
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @Json... 注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @Json... 注解
     */
    public static <S,T> T convertType(S source, TypeReference<T> type,
                                      boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        // noinspection DuplicatedCode
        if (U.isNull(source)) {
            return null;
        }

        String json;
        if (source instanceof String s) {
            json = s;
        } else {
            try {
                json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(source);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("obj({}) to json exception", U.compress(source.toString()), e);
                }
                return null;
            }
        }

        if (U.isBlank(json)) {
            return null;
        }
        try {
            return (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), type.getClass().getName(), e);
            }
            return null;
        }
    }

    /** 对象转换成 json 字符串 */
    public static String toJson(Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(String.format("object(%s) to json exception.", U.compress(obj.toString())), e);
        }
    }
    /** 对象转换成 json 字符串 */
    public static String toJsonNil(Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        if (obj instanceof String s) {
            return s;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Object({}) to json exception", U.compress(obj.toString()), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为对象 */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Class(%s) exception", U.compress(json), clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为对象, 当转换异常时, 返回 null */
    public static <T> T toObjectNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), clazz.getName(), e);
            }
            return null;
        }
    }
    /** 将 json 字符串转换为泛型对象 */
    public static <T> T toObjectType(String json, TypeReference<T> type) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), type.getClass().getName(), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为指定的集合, 转换失败则抛出 RuntimeException */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to List<%s> exception", U.compress(json), key), e);
        }
    }
    /** 将 json 字符串转换为指定的命令, 转换失败则返回空集合 */
    public static <T> List<T> toListNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(clazz.getName(),
                    fun -> OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyList();
        }
    }

    /** 将 json 字符串转换为指定的数组列表 */
    public static <K, V> Map<K, V> toMap(String json, final Class<K> keyClass, final Class<V> valueClass) {
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Map<%s> exception", U.compress(json), key), e);
        }
    }
    /** 将 json 字符串转换为指定的数组列表 */
    public static <K, V> Map<K, V> toMapNil(String json, final Class<K> keyClass, final Class<V> valueClass) {
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Map<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyMap();
        }
    }
}
