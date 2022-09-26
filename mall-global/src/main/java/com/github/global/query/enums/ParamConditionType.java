package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.util.QuerySqlUtil;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <pre>
 * global:
 *   is null      不常用
 *   is not null  不常用
 *   =
 *   <>
 *
 * list:
 *   in
 *   not in
 *
 * number/date:
 *   >
 *   >=
 *   <
 *   <=
 *   between
 *
 * string:
 *   like
 *   not like     不常用
 *
 *
 * string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(inc)、开头(sta)、结尾(end) 条件
 * number 类型: 只 等于(eq)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
 * date 类型: 只 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
 * 非 string/number/date 类型: 只 等于(eq)、不等于(ne) 条件
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ParamConditionType {

    /*
    eq(=)           : 等于
    ne(<>)          : 不等于
    in              : 批量(多个)
    bet(BETWEEN)    : 区间(时间或数字)
    gt(>)           : 大于
    ge(>=)          : 大于等于
    lt(<)           : 小于
    le(<=)          : 小于等于
    inc(LIKE '%x%') : 包含
    sta(LIKE 'x%')  : 开头
    end(LIKE '%x')  : 结尾

    下面几种是不常用的

    nu(IS NULL)     : 为空
    nn(IS NOT NULL) : 不为空
    ni(NOT IN)      : 不在其中(多个)
    nl(NOT LIKE ..) : 不包含
    */

    NU("IS NULL", "为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return String.format(" %s %s", QuerySqlUtil.toSqlField(column), getValue());
        }
    },
    NN("IS NOT NULL", "不为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return String.format(" %s %s", QuerySqlUtil.toSqlField(column), getValue());
        }
    },

    EQ("=", "等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    NE("<>", "不等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },

    IN("IN", "批量") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },
    NI("NOT IN", "不在其中") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },

    BET("BETWEEN", "区间") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateMulti(column, type, value, params);
        }
    },
    GT(">", "大于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    GE(">=", "大于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    LT("<", "小于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },
    LE("<=", "小于等于") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, value, params);
        }
    },

    INC("LIKE", "包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value + "%"), params);
        }
    },
    STA("LIKE", "开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, (value + "%"), params);
        }
    },
    END("LIKE", "结尾") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value), params);
        }
    },
    NL("NOT LIKE", "不包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value + "%"), params);
        }
    };


    @JsonValue
    private final String value;
    private final String msg;

    @JsonCreator
    public static ParamConditionType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ParamConditionType e : values()) {
                if (str.equalsIgnoreCase(e.name())) {
                    return e;
                }
                if (str.length() > 2 && str.toLowerCase().startsWith(e.name().toLowerCase())) {
                    return e;
                }
            }
        }
        return null;
    }


    public abstract String generateSql(String column, Class<?> type, Object value, List<Object> params);


    public String info() {
        return value + "(" + msg + ")";
    }

    public void checkTypeAndValue(Class<?> type, String column, Object value, int strLen) {
        checkType(type);
        checkValue(type, column, value, strLen);
    }


    protected String generateCondition(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null) {
            return "";
        } else {
            params.add(toValue(type, value));
            return String.format(" %s %s ?", column, getValue());
        }
    }
    protected String generateMulti(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null || !MULTI_TYPE.contains(this)) {
            return "";
        }
        Collection<?> c = (Collection<?>) value;
        if (c.isEmpty()) {
            return "";
        }

        if (this == BET) {
            Object[] arr = c.toArray();
            Object start = arr[0];
            Object end = arr.length > 1 ? arr[1] : null;

            StringBuilder sbd = new StringBuilder();
            if (start != null && end != null) {
                params.add(toValue(type, start));
                params.add(toValue(type, end));
                sbd.append(" ").append(column).append(" BETWEEN ? AND ?");
            } else {
                if (start != null) {
                    params.add(toValue(type, start));
                    sbd.append(" ").append(column).append(" >= ?");
                }
                if (end != null) {
                    params.add(toValue(type, end));
                    sbd.append(" ").append(column).append(" <= ?");
                }
            }
            return sbd.toString();
        } else {
            boolean hasChange = false;
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : c) {
                if (obj != null) {
                    if (!hasChange) {
                        hasChange = true;
                    }
                    sj.add("?");
                    params.add(toValue(type, obj));
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, getValue(), sj) : "";
        }
    }


    private static final Set<ParamConditionType> MULTI_TYPE = Set.of(
            ParamConditionType.IN,
            // ParamConditionType.NI,
            ParamConditionType.BET
    );
    /** string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(include)、开头(start)、结尾(end) 条件 */
    private static final Set<ParamConditionType> STRING_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.EQ,
            ParamConditionType.NE,
            ParamConditionType.IN,
            ParamConditionType.INC,
            ParamConditionType.STA,
            ParamConditionType.END
    ));
    private static final String STRING_TYPE_INFO = String.format("String type can only be used in 「%s」 conditions",
            STRING_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /** number 类型: 只 等于(eq)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(between) 条件 */
    private static final Set<ParamConditionType> NUMBER_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.EQ,
            ParamConditionType.GT,
            ParamConditionType.GE,
            ParamConditionType.LT,
            ParamConditionType.LE,
            ParamConditionType.BET
    ));
    private static final String NUMBER_TYPE_INFO = String.format("Number type can only be used in 「%s」 conditions",
            NUMBER_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /** date 类型: 只 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件 */
    private static final Set<ParamConditionType> DATE_TYPE_SET = new LinkedHashSet<>(Arrays.asList(
            ParamConditionType.GT,
            ParamConditionType.GE,
            ParamConditionType.LT,
            ParamConditionType.LE,
            ParamConditionType.BET
    ));
    private static final String DATE_TYPE_INFO = String.format("Date type can only be used in 「%s」 conditions",
            DATE_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    /**  非 string/number/date 类型: 只 等于(eq)、不等于(ne) 条件 */
    private static final Set<ParamConditionType> OTHER_TYPE_SET = Set.of(
            ParamConditionType.EQ,
            ParamConditionType.NE
    );
    private static final String OTHER_TYPE_INFO = String.format("Non(String, Number, Date) type can only be used in 「%s」 conditions",
            OTHER_TYPE_SET.stream().map(ParamConditionType::info).collect(Collectors.joining(", ")));

    private static final Set<Class<?>> BOOLEAN_TYPE_SET = new HashSet<>(Arrays.asList(Boolean.class, boolean.class));
    private static final Set<Class<?>> INT_TYPE_SET = new HashSet<>(Arrays.asList(Integer.class, int.class));
    private static final Set<Class<?>> LONG_TYPE_SET = new HashSet<>(Arrays.asList(Long.class, long.class));


    private void checkType(Class<?> type) {
        if (Number.class.isAssignableFrom(type)) {
            if (!NUMBER_TYPE_SET.contains(this)) {
                throw new RuntimeException(NUMBER_TYPE_INFO);
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (!DATE_TYPE_SET.contains(this)) {
                throw new RuntimeException(DATE_TYPE_INFO);
            }
        } else if (String.class.isAssignableFrom(type)) {
            if (!STRING_TYPE_SET.contains(this)) {
                throw new RuntimeException(STRING_TYPE_INFO);
            }
        } else {
            if (!OTHER_TYPE_SET.contains(this)) {
                throw new RuntimeException(OTHER_TYPE_INFO);
            }
        }
    }

    private void checkValue(Class<?> type, String column, Object value, int strLen) {
        if (value != null) {
            if (MULTI_TYPE.contains(this)) {
                if (value instanceof Collection<?> c) {
                    for (Object obj : c) {
                        if (obj != null) {
                            checkValueType(type, column, obj, strLen);
                        }
                    }
                } else {
                    throw new RuntimeException(String.format("column(%s) data need been Collection", column));
                }
            } else {
                checkValueType(type, column, value, strLen);
            }
        }
    }
    private void checkValueType(Class<?> type, String column, Object value, int strLen) {
        Object obj = toValue(type, value);
        if (obj == null) {
            throw new RuntimeException(String.format("column(%s) data(%s) has not %s type",
                    column, value, type.getSimpleName().toLowerCase()));
        }
        if (strLen > 0 && obj.toString().length() > strLen) {
            throw new RuntimeException(String.format("column(%s) data(%s) length can only be <= %s", column, value, strLen));
        }
    }

    private Object toValue(Class<?> type, Object value) {
        if (BOOLEAN_TYPE_SET.contains(type)) {
            return QueryUtil.isBoolean(value);
        } else if (INT_TYPE_SET.contains(type)) {
            return QueryUtil.toInteger(value);
        } else if (LONG_TYPE_SET.contains(type)) {
            return QueryUtil.toLonger(value);
        } else if (Number.class.isAssignableFrom(type)) {
            return QueryUtil.toDecimal(value);
        } else if (Date.class.isAssignableFrom(type)) {
            return QueryUtil.toDate(value);
        } else {
            return value;
        }
    }
}
