package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.*;

/**
 * <pre>
 * global:
 *   is null (inu)
 *   is not null (inn)
 *   = (eq)
 *   <> (ne)
 *
 * list:
 *   in (批量)
 *   not in (ni)
 *
 * number/date:
 *   > (gt)
 *   >= (ge)
 *   < (lt)
 *   <= (le)
 *   between 区间
 *
 * string:
 *   like (开头、结尾、包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
 *   not like (nl)
 *
 *
 * 针对业务系统时, 只需要:
 *   针对 string 类型的列提供 等于(eq)、批量(in)、包含(like)、开头(ll)、结尾(rl) 这几种方式
 *   针对 number/date 类型的列提供 等于(eq)、大于(>)、小于(<)、大于等于(>=)、小于等于(<=) 这几种方式
 * </pre>
 */
public enum ReqParamConditionType {

    IS_NULL("inu") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s IS NULL", column);
        }
    },
    IS_NOT_NULL("inn") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s IS NOT NULL", column);
        }
    },

    EQ("eq") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, "=");
        }
    },
    NOT_EQ("ne") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, "<>");
        }
    },

    IN("in") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMultiValue(column, value, params, "IN");
        }
    },
    NOT_IN("ni") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMultiValue(column, value, params, "NOT IN");
        }
    },

    BETWEEN("between") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMultiValue(column, value, params, "BETWEEN");
        }
    },
    GT("gt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, ">");
        }
    },
    GE("gt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, ">=");
        }
    },
    LT("lt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, "<");
        }
    },
    LE("le") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, value, params, "<=");
        }
    },

    LIKE("like") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, ("%" + value + "%"), params, "LIKE");
        }
    },
    LIKE_START("rl") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, (value + "%"), params, "LIKE");
        }
    },
    LIKE_END("ll") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, ("%" + value), params, "LIKE");
        }
    },
    NOT_LIKE("nl") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateValue(column, ("%" + value + "%"), params, "NOT LIKE");
        }
    }
    ;

    private static String generateValue(String column, Object value, List<Object> params, String symbol) {
        if (value == null) {
            return "";
        }

        params.add(value);
        return String.format(" %s %s ?", column, symbol);
    }
    private static String generateMultiValue(String column, Object value, List<Object> params, String symbol) {
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


    private final String value;
    ReqParamConditionType(String value) {
        this.value = value;
    }
    @JsonValue
    public String getValue() {
        return value;
    }


    abstract String generateSql(String column, Object value, List<Object> params);


    public void checkType(Class<?> type) {
        if (String.class.isAssignableFrom(type)) {
            if (!Set.of(EQ, IN, LIKE, LIKE_START, LIKE_END).contains(this)) {
                throw new RuntimeException(
                        String.format("「字符串」类型的列只能使用「等于(%s), 批量(%s), 包含(%s), 开头(%s), 结尾(%s)」的条件",
                                EQ.value, IN.value, LIKE.value, LIKE_START.value, LIKE_END.value));
            }
        } else if (Number.class.isAssignableFrom(type)) {
            if (!Set.of(EQ, GT, GE, LT, LE).contains(this)) {
                throw new RuntimeException(
                        String.format("「数字」类型的列只能使用「等于(%s), 大于(%s), 大于等于(%s), 小于(%s), 小于等于(%s)」的条件",
                                EQ.value, GT.value, GE.value, LT.value, LE.value));
            }
        } else if (Date.class.isAssignableFrom(type)) {
            if (!Set.of(GT, GE, LT, LE, BETWEEN).contains(this)) {
                throw new RuntimeException(
                        String.format("「日期时间」类型的列只能使用「大于(%s), 大于等于(%s), 小于(%s), 小于等于(%s), 区间(%s)」的条件",
                                GT.value, GE.value, LT.value, LE.value, BETWEEN.value));
            }
        } else {
            if (EQ != this) {
                throw new RuntimeException(String.format("非「字符串, 数字, 日期时间」类型的列只能使用「等于(%s)」的条件", EQ.value));
            }
        }
    }

    public static ReqParamConditionType getType(String type) {
        if (type != null && !type.isEmpty()) {
            for (ReqParamConditionType ct : values()) {
                if (ct.getValue().equalsIgnoreCase(type)) {
                    return ct;
                }
                if (ct.name().equalsIgnoreCase(type)) {
                    return ct;
                }
            }
        }
        return null;
    }
}
