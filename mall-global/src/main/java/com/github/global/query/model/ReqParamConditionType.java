package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.*;

/**
 * <pre>
 * global:
 *   is null     (inu)
 *   is not null (inn)
 *   =           (eq)
 *   <>          (ne)
 *
 * list:
 *   in     (批量)
 *   not in (ni)
 *
 * number/date:
 *   >  (gt)
 *   >= (ge)
 *   <  (lt)
 *   <= (le)
 *   between (bet)
 *
 * string:
 *   like     (开头、结尾、包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
 *   not like (nl)
 *
 *
 * 针对业务系统时, 只需要:
 *   针对 string 类型的列提供 等于(eq)、批量(in)、包含(like)、开头(ll)、结尾(rl) 这几种方式
 *   针对 number/date 类型的列提供 等于(eq)、大于(>)、小于(<)、大于等于(>=)、小于等于(<=) 这几种方式
 * </pre>
 */
public enum ReqParamConditionType {

    IS_NULL("inu", "为空") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s IS NULL", column);
        }
    },
    IS_NOT_NULL("inn", "不为空") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s IS NOT NULL", column);
        }
    },

    EQ("eq", "等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, "=");
        }
    },
    NOT_EQ("ne", "不等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, "<>");
        }
    },

    IN("in", "批量") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params, "IN");
        }
    },
    NOT_IN("ni", "不在列表") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params, "NOT IN");
        }
    },

    BETWEEN("bet", "区间") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params, "BETWEEN");
        }
    },
    GT("gt", "大于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, ">");
        }
    },
    GE("gt", "大于等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, ">=");
        }
    },
    LT("lt", "小于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, "<");
        }
    },
    LE("le", "小于等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params, "<=");
        }
    },

    LIKE("like", "包含") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value + "%"), params, "LIKE");
        }
    },
    LIKE_START("rl", "开头") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, (value + "%"), params, "LIKE");
        }
    },
    LIKE_END("ll", "结尾") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value), params, "LIKE");
        }
    },
    NOT_LIKE("nl", "不包含") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value + "%"), params, "NOT LIKE");
        }
    }
    ;


    private final String value;
    private final String msg;
    ReqParamConditionType(String value, String msg) {
        this.value = value;
        this.msg = msg;
    }
    @JsonValue
    public String getValue() {
        return value;
    }
    public String getMsg() {
        return msg;
    }


    abstract String generateSql(String column, Object value, List<Object> params);


    public void checkType(Class<?> type) {
        Map<Class<?>, Set<ReqParamConditionType>> conditionTypeMap = Map.of(
                String.class, Set.of(EQ, IN, LIKE, LIKE_START, LIKE_END),
                Number.class, Set.of(EQ, GT, GE, LT, LE),
                Date.class, Set.of(GT, GE, LT, LE, BETWEEN)
        );
        Map<Class<?>, String> typeInfoMap = new LinkedHashMap<>();
        typeInfoMap.put(String.class, "字符串");
        typeInfoMap.put(Number.class, "数字");
        typeInfoMap.put(Date.class, "日期时间");

        for (Map.Entry<Class<?>, Set<ReqParamConditionType>> entry : conditionTypeMap.entrySet()) {
            Class<?> clazz = entry.getKey();
            if (clazz.isAssignableFrom(type)) {
                Set<ReqParamConditionType> conditionTypes = entry.getValue();
                if (!conditionTypes.contains(this)) {
                    StringJoiner sj = new StringJoiner(", ");
                    for (ReqParamConditionType conditionType : conditionTypes) {
                        sj.add(String.format("%s(%s)", conditionType.msg, conditionType.value));
                    }
                    throw new RuntimeException(String.format("「%s」类型只能使用「%s」条件", typeInfoMap.get(clazz), sj));
                }
            }
        }

        Set<ReqParamConditionType> otherConditionType = Set.of(EQ);
        if (!otherConditionType.contains(this)) {
            StringJoiner sj = new StringJoiner(", ");
            for (ReqParamConditionType conditionType : otherConditionType) {
                sj.add(String.format("%s(%s)", conditionType.msg, conditionType.value));
            }

            StringJoiner typeJoin = new StringJoiner(", ");
            typeInfoMap.values().forEach(typeJoin::add);
            String otherTypeInfo = String.format("非「%s」", typeJoin);
            throw new RuntimeException(String.format("「%s」类型只能使用「%s」条件", otherTypeInfo, sj));
        }
    }

    public static ReqParamConditionType getType(String type) {
        if (type != null && !type.isEmpty()) {
            for (ReqParamConditionType conditionType : values()) {
                if (conditionType.value.equalsIgnoreCase(type)) {
                    return conditionType;
                }
                if (conditionType.name().equalsIgnoreCase(type)) {
                    return conditionType;
                }
            }
        }
        return null;
    }

    private static String generateCondition(String column, Object value, List<Object> params, String symbol) {
        if (value == null) {
            return "";
        }

        params.add(value);
        return String.format(" %s %s ?", column, symbol);
    }
    private static String generateMulti(String column, Object value, List<Object> params, String symbol) {
        if (value == null) {
            return "";
        }

        if (value instanceof Collection<?> c) {
            if (c.isEmpty()) {
                return "";
            }
            if ("BETWEEN".equalsIgnoreCase(symbol)) {
                Object[] arr = c.toArray();
                Object start = arr[0];
                Object end = arr.length > 1 ? arr[1] : null;

                StringBuilder sbd = new StringBuilder();
                if (start != null) {
                    params.add(start);
                    sbd.append(" ").append(column).append(" >= ?");
                }
                if (end != null) {
                    params.add(end);
                    if (sbd.length() > 0) {
                        sbd.append(" AND");
                    }
                    sbd.append(" ").append(column).append(" <= ?");
                }
                // 用 >= ? AND <= ? 来实现 BETWEEN ? AND ?
                return sbd.toString();
            } else {
                boolean hasChange = false;
                StringJoiner sj = new StringJoiner(", ");
                for (Object obj : c) {
                    if (obj != null) {
                        hasChange = true;
                        sj.add("?");
                        params.add(obj);
                    }
                }
                return hasChange ? String.format(" %s %s (%s)", column, symbol, sj) : "";
            }
        } else {
            throw new RuntimeException("数据需要是集合");
        }
    }
}
