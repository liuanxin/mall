package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * FROM order
 * WHERE ...
 * ORDER BY create_time DESC, id ASC
 * LIMIT 20
 * {
 *   "query": ...
 *   "sort": { "createTime": "desc", "id", "asc" },
 *   "page": [ 1, 20 ],
 *   "notCount": true  -- true 表示不发起 count 查询叫条数, 不设置则默认是 false
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
    /** 当上面的分页数据有值, 当前值是 true 时表示不发起 count 查询总条数, 在「移动端-瀑布流」时是「无需查询总条数」的 */
    private Boolean notCount;


    public Set<String> checkParam(String mainSchema, SchemaColumnInfo schemaColumnInfo) {
        Set<String> paramSchemaSet = new LinkedHashSet<>();
        paramSchemaSet.add(mainSchema);
        if (query != null) {
            query.checkCondition(mainSchema, schemaColumnInfo);
            query.allSchema(mainSchema, paramSchemaSet);
        }

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                QueryUtil.checkColumnName(column, mainSchema, schemaColumnInfo, "query order");

                if (!column.contains(".")) {
                    String orderSchemaName = QueryUtil.getSchemaName(column, mainSchema);
                    if (!paramSchemaSet.contains(orderSchemaName)) {
                        throw new RuntimeException("no order schema(" + orderSchemaName + ") in query condition");
                    }
                    paramSchemaSet.add(orderSchemaName);
                }
            }
        }

        if (needQueryPage()) {
            Integer indexParam = page.get(0);
            if (indexParam == null || indexParam <= 0) {
                throw new RuntimeException("param page error");
            }
        }

        paramSchemaSet.remove(mainSchema);
        if (paramSchemaSet.size() > 1) {
            for (String schemaName : paramSchemaSet) {
                if (schemaColumnInfo.findSchema(schemaName) == null) {
                    throw new RuntimeException("param no relation " + schemaName + " defined");
                }
                if (schemaColumnInfo.findRelationByMasterChild(mainSchema, schemaName) == null) {
                    throw new RuntimeException("param " + mainSchema + " is not related to " + schemaName);
                }
            }
        }
        return paramSchemaSet;
    }

    public String generateWhereSql(String mainSchema, SchemaColumnInfo schemaColumnInfo, List<Object> params, boolean needAlias) {
        if (query == null) {
            return "";
        }

        String where = query.generateSql(mainSchema, schemaColumnInfo, params, needAlias);
        return where.isEmpty() ? "" : (" WHERE " + where);
    }

    public String generateOrderSql(String mainSchema, boolean needAlias, SchemaColumnInfo schemaColumnInfo) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getRealColumn(needAlias, entry.getKey(), mainSchema, schemaColumnInfo) + desc);
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
    public boolean needQueryCount() {
        return notCount == null || !notCount;
    }
    public boolean needQueryCurrentPage(long count) {
        if (count <= 0) {
            return false;
        }
        int index = page.get(0);
        int limit = calcLimit();
        // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
        return ((long) index * limit) <= count;
    }
    private int calcLimit() {
        Integer limitParam = page.size() > 1 ? page.get(1) : 0;
        return QueryConst.LIMIT_SET.contains(limitParam) ? limitParam : QueryConst.MIN_LIMIT;
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
    public String generateArrToObjSql(List<Object> params) {
        params.add(1);
        return " LIMIT ?";
    }

    public boolean hasDeepPage(int maxSize) {
        return needQueryPage() && (((page.get(0) - 1) * calcLimit()) > maxSize);
    }
}
