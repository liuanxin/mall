package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * {
 *   "query": ...
 *   "sort": { "createTime": "desc", "id", "asc" },
 *   "page": [ 1, 20 ]
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
public class ReqParam {

    /** 查询信息 */
    private ReqParamOperate query;

    /** 排序信息 */
    private Map<String, String> sort;

    /** 分页信息 */
    private List<Integer> page;


    public void checkParam(String mainSchema, SchemaColumnInfo columnInfo) {
        if (query == null) {
            throw new RuntimeException("param no query");
        }
        query.checkCondition(mainSchema, columnInfo);

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                QueryUtil.checkColumnName(column, mainSchema, columnInfo, "query order");
            }
        }

        if (needQueryPage()) {
            Integer indexParam = page.get(0);
            if (indexParam == null || indexParam <= 0) {
                throw new RuntimeException("param page error");
            }
        }
    }

    public Set<String> allParamSchema(String mainSchema) {
        Set<String> set = new LinkedHashSet<>();
        query.allSchema(mainSchema, set);
        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                set.add(QueryUtil.getSchemaName(column, mainSchema));
            }
        }
        return set;
    }

    public String generateOrderSql(String mainSchema, Map<String, Schema> schemaMap) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                orderSj.add(entry.getKey() + " " + ("asc".equalsIgnoreCase(entry.getValue()) ? "ASC" : "DESC"));
            }
            String orderBy = orderSj.toString();
            if (!orderBy.isEmpty()) {
                return " ORDER BY " + orderBy;
            }
        }
        return "";
    }

    public boolean needQueryPage() {
        return page != null && !page.isEmpty();
    }
    public String generatePageSql(List<Object> params) {
        if (needQueryPage()) {
            int index = page.get(0);
            int limit = calcLimit();

            if (index == 1) {
                params.add(limit);
                return " LIMIT ?";
            } else {
                params.add((index - 1) * limit);
                params.add(limit);
                return " LIMIT ?, ?";
            }
        }
        return "";
    }
    private int calcLimit() {
        Integer limitParam = page.size() > 1 ? page.get(1) : 0;
        return QueryConst.LIMIT_SET.contains(limitParam) ? limitParam : QueryConst.MIN_LIMIT;
    }
    public boolean needQueryCurrentPage(long count) {
        if (needQueryPage()) {
            int index = page.get(0);
            int limit = calcLimit();
            // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
            return ((long) index * limit) <= count;
        }
        return false;
    }
}
