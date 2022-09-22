package com.github.global.query.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QuerySqlUtil;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * <pre>
 * global:
 *   is null     (inu)
 *   is not null (inn)
 *   =           (eq)
 *   <>          (ne)
 *
 * list:
 *   in     (in 批量)
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
 *   like     (lks 开头、lke 结尾、lk 包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
 *   not like (nl)
 *
 *
 * string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(lk)、开头(lks)、结尾(lke)、不包含(nl) 条件
 * number 类型: 只 等于(eq)、大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
 * date 类型: 只 大于(gt)、大于等于(ge)、小于(lt)、小于等于(le)、区间(bet) 条件
 * 非 string/number/date 类型: 只 等于(eq)、不等于(ne) 条件
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ReqParamConditionType {

    INU("IS NULL", "为空") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return String.format(" %s %s", QuerySqlUtil.toSqlField(column), getValue());
        }
    },
    INN("IS NOT NULL", "不为空") {
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
    NI("NOT IN", "不在") {
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

    LK("LIKE", "包含") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, ("%" + value + "%"), params);
        }
    },
    LKS("LIKE", "开头") {
        @Override
        public String generateSql(String column, Class<?> type, Object value, List<Object> params) {
            return generateCondition(column, type, (value + "%"), params);
        }
    },
    LKE("LIKE", "结尾") {
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
    public static ReqParamConditionType deserializer(Object obj) {
        if (obj != null) {
            String str = obj.toString().trim();
            for (ReqParamConditionType e : values()) {
                if (str.equalsIgnoreCase(e.name()) || str.equalsIgnoreCase(e.value)) {
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
        QueryUtil.checkParamType(type, this);
        checkValue(type, column, value, strLen);
    }

    private void checkValue(Class<?> type, String column, Object value, int strLen) {
        if (value != null) {
            if (QueryConst.MULTI_TYPE.contains(this)) {
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
        Object obj = QueryUtil.toValue(type, value);
        if (obj == null) {
            throw new RuntimeException(String.format("column(%s) data(%s) has not %s type",
                    column, value, type.getSimpleName().toLowerCase()));
        }
        if (strLen > 0 && obj.toString().length() > strLen) {
            throw new RuntimeException(String.format("column(%s) data(%s) length can only be <= %s", column, value, strLen));
        }
    }

    protected String generateCondition(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null) {
            return "";
        } else {
            params.add(QueryUtil.toValue(type, value));
            return String.format(" %s %s ?", column, getValue());
        }
    }
    protected String generateMulti(String column, Class<?> type, Object value, List<Object> params) {
        if (value == null || !QueryConst.MULTI_TYPE.contains(this)) {
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
                params.add(QueryUtil.toValue(type, start));
                params.add(QueryUtil.toValue(type, end));
                sbd.append(" ").append(column).append(" BETWEEN ? AND ?");
            } else {
                if (start != null) {
                    params.add(QueryUtil.toValue(type, start));
                    sbd.append(" ").append(column).append(" >= ?");
                }
                if (end != null) {
                    params.add(QueryUtil.toValue(type, end));
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
                    params.add(QueryUtil.toValue(type, obj));
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, getValue(), sj) : "";
        }
    }
}
