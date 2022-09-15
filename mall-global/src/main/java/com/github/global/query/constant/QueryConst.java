package com.github.global.query.constant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.global.query.enums.ReqParamConditionType;
import com.github.global.query.model.ReqResult;

import java.math.BigDecimal;
import java.util.*;

public final class QueryConst {

    public static final int MAX_PAGE_SIZE = 10000;

    public static final String DB_SQL = "SELECT DATABASE()";
    public static final String SCHEMA_SQL = "SELECT `TABLE_NAME` tn, `TABLE_COMMENT` tc" +
            " FROM information_schema.`TABLES` WHERE `TABLE_SCHEMA` = ?";
    public static final String COLUMN_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, `COLUMN_TYPE` ct," +
            " `COLUMN_COMMENT` cc, `COLUMN_KEY` ck" +
            " FROM `information_schema`.`COLUMNS` WHERE `TABLE_SCHEMA` = ?" +
            " ORDER BY `TABLE_NAME`, `ORDINAL_POSITION`";
    public static final String RELATION_SQL = "SELECT `REFERENCED_TABLE_NAME` ftn, `REFERENCED_COLUMN_NAME` fcn," +
            " `TABLE_NAME` tn, `COLUMN_NAME` cn" +
            " FROM `information_schema`.`KEY_COLUMN_USAGE`" +
            " WHERE `REFERENCED_TABLE_SCHEMA` = ?";
    public static final String INDEX_SQL = "SELECT `TABLE_NAME` tn, `COLUMN_NAME` cn, COUNT(`SEQ_IN_INDEX`) sii" +
            " FROM `INFORMATION_SCHEMA`.`STATISTICS`" +
            " WHERE `NON_UNIQUE` = 0 AND `TABLE_SCHEMA` = ?" +
            " GROUP BY tn, cn HAVING sii = 1";

    public static final Map<String, Class<?>> DB_TYPE_MAP = new LinkedHashMap<>();
    static {
        DB_TYPE_MAP.put("tinyint(1)", Boolean.class);
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

    public static final TypeReference<Map<String, ReqResult>> RESULT_TYPE = new TypeReference<>() {};

    public static final String SCHEMA_PREFIX = "schema-";
    public static final String COLUMN_PREFIX = "column-";


    public static final Integer MIN_LIMIT = 20;
    public static final Set<Integer> LIMIT_SET = new HashSet<>(Arrays.asList(
            MIN_LIMIT, 50, 100, 200, 500, 1000)
    );


    public static final Map<Class<?>, String> TYPE_INFO_MAP = Map.of(
            String.class, "「字符串」",
            Number.class, "「数字」",
            Date.class, "「日期时间」"
    );
    /**
     * <pre>
     * string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(like)、开头(ll)、结尾(rl)、不包含(nl) 条件
     * number 类型: 只 等于(eq)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
     * date 类型: 只 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
     * </pre>
     */
    public static final Map<Class<?>, Set<ReqParamConditionType>> CONDITION_TYPE_MAP = Map.of(
            String.class, new LinkedHashSet<>(Arrays.asList(
                    ReqParamConditionType.EQ,
                    ReqParamConditionType.NE,
                    ReqParamConditionType.IN,
                    ReqParamConditionType.LK,
                    ReqParamConditionType.LKS,
                    ReqParamConditionType.LKE,
                    ReqParamConditionType.NL
            )),
            Number.class, new LinkedHashSet<>(Arrays.asList(
                    ReqParamConditionType.EQ,
                    ReqParamConditionType.GT,
                    ReqParamConditionType.GE,
                    ReqParamConditionType.LT,
                    ReqParamConditionType.LE,
                    ReqParamConditionType.BET
            )),
            Date.class, new LinkedHashSet<>(Arrays.asList(
                    ReqParamConditionType.GT,
                    ReqParamConditionType.GE,
                    ReqParamConditionType.LT,
                    ReqParamConditionType.LE,
                    ReqParamConditionType.BET
            ))
    );
    public static final String OTHER_TYPE_INFO = "非「字符串, 数字, 日期时间」";
    /**  非 string/number/date 类型: 只 等于(eq)、不等于(ne) 条件 */
    public static final Set<ReqParamConditionType> OTHER_CONDITION_TYPE = Set.of(
            ReqParamConditionType.EQ,
            ReqParamConditionType.NE
    );

    public static final Set<ReqParamConditionType> MULTI_TYPE = Set.of(
            ReqParamConditionType.IN,
            ReqParamConditionType.NI,
            ReqParamConditionType.BET
    );
}
