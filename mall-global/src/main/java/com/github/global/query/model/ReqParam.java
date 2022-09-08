package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.util.QueryUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * <pre>
 * FROM order
// *   LEFT JOIN orderAddress ON ...
// *   LEFT JOIN orderItem ON ...
// *   RIGHT JOIN userInfo ON ...
// *   RIGHT JOIN user ON ...
 * WHERE ...
 * ORDER BY create_time DESC, id ASC
 * LIMIT 20
 * {
// *   "relation": { "left": [ "orderAddress", "orderItem" ], "right": [ "userInfo", "user" ] },
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

//    /** 结构关联方式 */
//    private Map<ReqParamJoinType, List<String>> relation;

    /** 查询信息 */
    private ReqParamOperate query;

    /** 排序信息 */
    private Map<String, String> sort;

    /** 分页信息 */
    private List<Integer> page;

    /** 当上面的分页数据有值, 当前值是 true 时表示不发起 count 查询总条数, 在「移动端-瀑布流」时是「无需查询总条数」的 */
    private Boolean notCount;


    public void checkParam(String mainSchema, SchemaColumnInfo columnInfo) {
//        if (relation != null && !relation.isEmpty()) {
//            List<String> relationList = new ArrayList<>();
//            for (List<String> list : relation.values()) {
//                for (String str : list) {
//                    if (columnInfo.findSchema(str) == null) {
//                        relationList.add(str);
//                    }
//                }
//            }
//            if (!relationList.isEmpty()) {
//                throw new RuntimeException("param relation schema" + relationList + " no defined");
//            }
//        }
        if (query == null) {
            throw new RuntimeException("param no query");
        }
        query.checkCondition(mainSchema, columnInfo);

        if (sort != null && !sort.isEmpty()) {
            Set<String> useSchemaSet = allParamSchema(mainSchema);
            for (String column : sort.keySet()) {
                QueryUtil.checkColumnName(column, mainSchema, columnInfo, "query order");

                if (!column.contains(".")) {
                    String orderSchemaName = QueryUtil.getSchemaName(column, mainSchema);
                    if (!useSchemaSet.contains(orderSchemaName)) {
                        throw new RuntimeException("no order schema(" + orderSchemaName + ") in query condition");
                    }
                }
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

    public String generateOrderSql() {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                orderSj.add(entry.getKey() + ("asc".equalsIgnoreCase(entry.getValue()) ? " ASC" : " DESC"));
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
    private int calcLimit() {
        Integer limitParam = page.size() > 1 ? page.get(1) : 0;
        return QueryConst.LIMIT_SET.contains(limitParam) ? limitParam : QueryConst.MIN_LIMIT;
    }
    public boolean needQueryCurrentPage(long count) {
        int index = page.get(0);
        int limit = calcLimit();
        // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
        return ((long) index * limit) <= count;
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
}
