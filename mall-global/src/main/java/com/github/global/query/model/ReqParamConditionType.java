package com.github.global.query.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
 *   like     (rl 开头、ll 结尾、lk 包含), 只有「开头」会走索引(LIKE 'x%'), 结尾是 LIKE '%xx', 包含是 LIKE '%xxx%'
 *   not like (nl)
 *
 *
 * string 类型: 只 等于(eq)、不等于(ne)、批量(in)、包含(lk)、开头(rl)、结尾(ll)、不包含(nl) 条件
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
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s %s", column, getValue());
        }
    },
    INN("IS NOT NULL", "不为空") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return String.format(" %s %s", column, getValue());
        }
    },

    EQ("=", "等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },
    NE("<>", "不等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },

    IN("IN", "批量") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params);
        }
    },
    NI("NOT IN", "不在") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params);
        }
    },

    BET("BETWEEN", "区间") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateMulti(column, value, params);
        }
    },
    GT(">", "大于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },
    GE(">=", "大于等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },
    LT("<", "小于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },
    LE("<=", "小于等于") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, value, params);
        }
    },

    LK("LIKE", "包含") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value + "%"), params);
        }
    },
    LKS("LIKE", "开头") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, (value + "%"), params);
        }
    },
    LKE("LIKE", "结尾") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value), params);
        }
    },
    NL("NOT LIKE", "不包含") {
        @Override
        public String generateSql(String column, Object value, List<Object> params) {
            return generateCondition(column, ("%" + value + "%"), params);
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


    public abstract String generateSql(String column, Object value, List<Object> params);


    public void checkTypeAndValue(Class<?> type, String column, Object value) {
        checkType(type);
        checkValue(type, column, value);
    }

    private void checkType(Class<?> type) {
        for (Map.Entry<Class<?>, Set<ReqParamConditionType>> entry : QueryConst.CONDITION_TYPE_MAP.entrySet()) {
            Class<?> clazz = entry.getKey();
            if (clazz.isAssignableFrom(type)) {
                checkTypes(QueryConst.TYPE_INFO_MAP.get(clazz), entry.getValue());
            }
        }

        checkTypes(QueryConst.OTHER_TYPE_INFO, QueryConst.OTHER_CONDITION_TYPE);
    }
    private void checkTypes(String typeInfo, Set<ReqParamConditionType> types) {
        if (!types.contains(this)) {
            StringJoiner sj = new StringJoiner(", ");
            for (ReqParamConditionType conditionType : types) {
                sj.add(String.format("%s(%s)", conditionType.msg, conditionType.value));
            }
            throw new RuntimeException(String.format("%s type can only be used in 「%s」 conditions", typeInfo, sj));
        }
    }

    private void checkValue(Class<?> type, String column, Object value) {
        if (value != null) {
            if (QueryConst.MULTI_TYPE.contains(this)) {
                if (value instanceof Collection<?> c) {
                    StringJoiner errorSj = new StringJoiner(", ");
                    for (Object obj : c) {
                        if (obj != null && !type.isAssignableFrom(obj.getClass())) {
                            errorSj.add(obj.toString());
                        }
                    }
                    String error = errorSj.toString();
                    if (!error.isEmpty()) {
                        throw new RuntimeException(String.format("column(%s) data(%s) error", column, error));
                    }
                } else {
                    throw new RuntimeException(String.format("column(%s) data need been Collection", column));
                }
            } else {
                if (!type.isAssignableFrom(value.getClass())) {
                    throw new RuntimeException(String.format("column(%s) data(%s) error", column, value));
                }
            }
        }
    }


    protected String generateCondition(String column, Object value, List<Object> params) {
        if (value == null || QueryUtil.isNullString(value)) {
            return "";
        }

        params.add(value);
        return String.format(" %s %s ?", column, getValue());
    }
    protected String generateMulti(String column, Object value, List<Object> params) {
        if (value == null || !QueryConst.MULTI_TYPE.contains(this)) {
            return "";
        }

        Collection<?> c = (Collection<?>) value;
        if (c.isEmpty()) {
            return "";
        }
        String symbol = getValue();
        if ("BETWEEN".equals(symbol)) {
            Object[] arr = c.toArray();
            Object start = arr[0];
            Object end = arr.length > 1 ? arr[1] : null;

            StringBuilder sbd = new StringBuilder();
            if (start != null && end != null) {
                params.add(start);
                params.add(end);
                sbd.append(" ").append(column).append(" BETWEEN ? AND ?");
            } else {
                if (start != null) {
                    params.add(start);
                    sbd.append(" ").append(column).append(" >= ?");
                }
                if (end != null) {
                    params.add(end);
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
                    params.add(obj);
                }
            }
            return hasChange ? String.format(" %s %s (%s)", column, symbol, sj) : "";
        }
    }
}
