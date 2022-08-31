package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.A;
import com.github.common.util.U;

import java.lang.reflect.Array;
import java.util.*;

/**
 * <pre>
 * global:
 *   is null (inu)
 *   is not null (nn)
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
 *   between (btn)
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
    IS_NOT_NULL("nn") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s IS NOT NULL", column);
        }
    },

    EQ("eq") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s = ?", column);
        }
    },
    NOT_EQ("ne") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s <> ?", column);
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

    GT("gt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s > ?", column);
        }
    },
    GE("gt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s >= ?", column);
        }
    },
    LT("lt") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s < ?", column);
        }
    },
    LE("le") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value);
            return String.format(" %s <= ?", column);
        }
    },
    BETWEEN("btn") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            U.assertException(A.isNotArray(value), "generate symbol BETWEEN need Array or Collection");

            Object start, end;
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                if (length != 2) {
                    return "";
                }
                start = Array.get(value, 0);
                end = Array.get(value, 1);
            }
            else if (value instanceof Collection<?> c) {
                if (c.size() != 2) {
                    return "";
                }
                Object[] arr = c.toArray();
                start = arr[0];
                end = arr[1];
            }
            else {
                start = end = null;
            }
            StringBuilder sbd = new StringBuilder();
            if (U.isNotNull(start)) {
                params.add(start);
                sbd.append(" ").append(column).append(" >= ?");
                if (U.isNotNull(end)) {
                    sbd.append(" AND");
                }
            }
            if (U.isNotNull(end)) {
                params.add(end);
                sbd.append(" ").append(column).append(" <= ?");
            }
            // return String.format(" %s BETWEEN ? AND ?", column);
            return sbd.toString();
        }
    },

    LIKE("like") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add("%" + value + "%");
            return String.format(" %s LIKE ?", column);
        }
    },
    LIKE_START("rl") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add(value + "%");
            return String.format(" %s LIKE ?", column);
        }
    },
    LIKE_END("ll") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add("%" + value);
            return String.format(" %s LIKE ?", column);
        }
    },
    NOT_LIKE("nl") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            params.add("%" + value + "%");
            return String.format(" %s NOT LIKE ?", column);
        }
    }
    ;


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
            U.assertException(!Set.of(EQ, IN, LIKE, LIKE_START, LIKE_END).contains(this),
                    String.format("「字符串」类型的列只能使用「等于(%s), 批量(%s), 包含(%s), 开头(%s), 结尾(%s)」的条件",
                            EQ.value, IN.value, LIKE.value, LIKE_START.value, LIKE_END.value));
        } else if (Number.class.isAssignableFrom(type)) {
            U.assertException(!Set.of(EQ, GT, GE, LT, LE).contains(this),
                    String.format("「数字」类型的列只能使用「等于(%s), 大于(%s), 大于等于(%s), 小于(%s), 小于等于(%s)」的条件",
                            EQ.value, GT.value, GE.value, LT.value, LE.value));
        } else if (Date.class.isAssignableFrom(type)) {
            U.assertException(!Set.of(GT, GE, LT, LE, BETWEEN).contains(this),
                    String.format("「日期时间」类型的列只能使用「大于(%s), 大于等于(%s), 小于(%s), 小于等于(%s), 区间(%s)」的条件",
                            GT.value, GE.value, LT.value, LE.value, BETWEEN.value));
        } else {
            U.assertException(EQ != this, String.format("非「字符串, 数字, 日期时间」类型的列只能使用「等于(%s)」的条件", EQ.value));
        }
    }

    public static ReqParamConditionType getType(String type) {
        if (U.isNotBlank(type)) {
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

    private static String generateMultiValue(String column, Object value, List<Object> params, String symbol) {
        U.assertException(A.isNotArray(value), "generate symbol " + symbol + " need Array or Collection");

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length == 0) {
                return "";
            }

            boolean hasChange = false;
            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < length; i++) {
                Object obj = Array.get(value, i);
                if (U.isNotNull(obj)) {
                    hasChange = true;
                    sj.add("?");
                    params.add(obj);
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, symbol, sj) : "";
        }
        else if (value instanceof Collection<?> c) {
            if (c.isEmpty()) {
                return "";
            }

            boolean hasChange = false;
            StringJoiner sj = new StringJoiner(", ");
            for (Object obj : c) {
                if (U.isNotNull(obj)) {
                    hasChange = true;
                    sj.add("?");
                    params.add(obj);
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, symbol, sj) : "";
        }
        else {
            return "";
        }
    }
}
