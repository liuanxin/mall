package com.github.global.query.constant;

import com.github.global.query.enums.ParamConditionType;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public final class QueryConst {

    public static final String DB_SQL = "SELECT DATABASE()";
    public static final String SCHEMA_SQL = "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc" +
            " FROM `information_schema`.`TABLES`" +
            " WHERE `TABLE_SCHEMA` = ?";
    public static final String COLUMN_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct," +
            " `COLUMN_COMMENT` cc, `COLUMN_KEY` ck, `CHARACTER_MAXIMUM_LENGTH` cml" +
            " FROM `information_schema`.`COLUMNS`" +
            " WHERE `TABLE_SCHEMA` = ?" +
            " ORDER BY `TABLE_NAME`, `ORDINAL_POSITION`";
    public static final String RELATION_SQL = "SELECT `REFERENCED_TABLE_NAME` ftn, `REFERENCED_COLUMN_NAME` fcn," +
            " `TABLE_NAME` tn, `COLUMN_NAME` cn" +
            " FROM `information_schema`.`KEY_COLUMN_USAGE`" +
            " WHERE `REFERENCED_TABLE_SCHEMA` = ?";
    public static final String INDEX_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn" +
            " FROM `information_schema`.`STATISTICS`" +
            " WHERE `NON_UNIQUE` = 0 AND `TABLE_SCHEMA` = ?" +
            " GROUP BY tn, cn" +
            " HAVING COUNT(`SEQ_IN_INDEX`) = 1";

    public static final Map<String, Class<?>> DB_TYPE_MAP = new LinkedHashMap<>();
    static {
        DB_TYPE_MAP.put("tinyint(1) unsigned", Integer.class);
        DB_TYPE_MAP.put("tinyint(1)", Boolean.class);
        DB_TYPE_MAP.put("bigint", Long.class);
        DB_TYPE_MAP.put("int", Integer.class);

        DB_TYPE_MAP.put("char", String.class);
        DB_TYPE_MAP.put("text", String.class);

        DB_TYPE_MAP.put("date", Date.class);
        DB_TYPE_MAP.put("time", Date.class);
        DB_TYPE_MAP.put("year", Date.class);

        DB_TYPE_MAP.put("decimal", BigDecimal.class);
        DB_TYPE_MAP.put("float", Float.class);
        DB_TYPE_MAP.put("double", Double.class);
    }

    public static final String SCHEMA_PREFIX = "schema-";
    public static final String COLUMN_PREFIX = "column-";


    public static final Integer MIN_LIMIT = 10;
    public static final Set<Integer> LIMIT_SET = new HashSet<>(Arrays.asList(
            MIN_LIMIT, 20, 50, 100, 200, 500, 1000)
    );


    /** string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(like)、开头(ll)、结尾(rl)、不包含(nl) 条件 */
    public static final Set<ParamConditionType> STRING_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.EQ,
            ParamConditionType.NE,
            ParamConditionType.IN,
            ParamConditionType.LK,
            ParamConditionType.LKS,
            ParamConditionType.LKE,
            ParamConditionType.NL
    ));
    public static final String STRING_TYPE_INFO = String.format("String type can only be used in 「%s」 conditions",
            STRING_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /** number 类型: 只 等于(eq)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件 */
    public static final Set<ParamConditionType> NUMBER_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.EQ,
            ParamConditionType.GT,
            ParamConditionType.GE,
            ParamConditionType.LT,
            ParamConditionType.LE,
            ParamConditionType.BET
    ));
    public static final String NUMBER_TYPE_INFO = String.format("Number type can only be used in 「%s」 conditions",
            NUMBER_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /** date 类型: 只 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件 */
    public static final Set<ParamConditionType> DATE_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.GT,
            ParamConditionType.GE,
            ParamConditionType.LT,
            ParamConditionType.LE,
            ParamConditionType.BET
    ));
    public static final String DATE_TYPE_INFO = String.format("Date type can only be used in 「%s」 conditions",
            DATE_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /**  非 string/number/date 类型: 只 等于(eq)、不等于(ne) 条件 */
    public static final Set<ParamConditionType> OTHER_TYPE_SET = Set.of(
            ParamConditionType.EQ,
            ParamConditionType.NE
    );
    public static final String OTHER_TYPE_INFO = String.format("Non(String, Number, Date) type can only be used in 「%s」 conditions",
            OTHER_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    public static final Set<ParamConditionType> MULTI_TYPE = Set.of(
            ParamConditionType.IN,
            ParamConditionType.NI,
            ParamConditionType.BET
    );

    public static final Set<Class<?>> BOOLEAN_TYPE_SET = new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
    public static final Set<Class<?>> INT_TYPE_SET = new HashSet<>(Arrays.asList(Integer.class, int.class));
    public static final Set<Class<?>> LONG_TYPE_SET = new HashSet<>(Arrays.asList(Long.class, long.class));

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final List<String> DATE_FORMAT_LIST = Arrays.asList(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy/MM/dd",
            DEFAULT_DATE_FORMAT,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "HH:mm:ss",
            "HH:mm"
    );
}
