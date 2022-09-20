package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqJoinType;
import com.github.global.query.enums.TableRelationType;
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

    /** { [ "order", "left", "orderItem" ] , [ "order", "right", "orderPrice" ] ] */
    private List<List<String>> relation;
    /** 查询信息 */
    private ReqParamOperate query;
    /** 排序信息 */
    private Map<String, String> sort;
    /** 分页信息 */
    private List<Integer> page;
    /** 当上面的分页数据有值, 当前值是 true 时表示不发起 count 查询总条数, 在「移动端-瀑布流」时是「无需查询总条数」的 */
    private Boolean notCount;


    public void checkParam(String mainTable, TableColumnInfo tableColumnInfo) {
        if (query != null) {
            query.checkCondition(mainTable, tableColumnInfo);
        }

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                QueryUtil.checkColumnName(column, mainTable, tableColumnInfo, "param order");
            }
        }

        if (needQueryPage()) {
            Integer indexParam = page.get(0);
            if (indexParam == null || indexParam <= 0) {
                throw new RuntimeException("param page error");
            }
        }

        if (relation != null && !relation.isEmpty()) {
            Set<String> tableRelation = new HashSet<>();
            for (List<String> values : relation) {
                if (values.size() >= 3) {
                    ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                    if (joinType != null) {
                        String masterTable = values.get(0);
                        String childTable = values.get(2);
                        if (tableColumnInfo.findRelationByMasterChild(masterTable, childTable) == null) {
                            throw new RuntimeException(masterTable + " and " + childTable + " has no relation");
                        }

                        String key = masterTable + "." + childTable;
                        if (tableRelation.contains(key)) {
                            throw new RuntimeException(masterTable + " and " + childTable + " can only has one relation");
                        }
                        tableRelation.add(key);
                    }
                }
            }
        }
    }

    public Map<String, Set<TableJoinRelation>> joinRelationMap(TableColumnInfo tableColumnInfo) {
        if (relation.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        for (List<String> values : relation) {
            if (values.size() >= 3) {
                ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                if (joinType != null) {
                    Table masterTable = tableColumnInfo.findTable(values.get(0));
                    Table childTable = tableColumnInfo.findTable(values.get(2));

                    String masterTableName = masterTable.getName();
                    Set<TableJoinRelation> relationSet = relationMap.getOrDefault(masterTableName, new LinkedHashSet<>());
                    relationSet.add(new TableJoinRelation(masterTable, joinType, childTable));
                    relationMap.put(masterTableName, relationSet);
                }
            }
        }
        return relationMap;
    }

    public Set<String> allParamTable(TableColumnInfo tableColumnInfo) {
        if (relation.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> tableSet = new LinkedHashSet<>();
        for (List<String> values : relation) {
            if (values.size() >= 3) {
                ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                if (joinType != null) {
                    Table masterTable = tableColumnInfo.findTable(values.get(0));
                    Table childTable = tableColumnInfo.findTable(values.get(2));
                    tableSet.add(masterTable.getName());
                    tableSet.add(childTable.getName());
                }
            }
        }
        return tableSet;
    }

    public boolean hasManyRelation(TableColumnInfo tableColumnInfo) {
        if (relation.isEmpty()) {
            return false;
        }

        for (List<String> values : relation) {
            ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
            if (joinType != null) {
                String masterTableName = values.get(0);
                String childTableName = values.get(2);
                TableColumnRelation relation = tableColumnInfo.findRelationByMasterChild(masterTableName, childTableName);
                if (relation != null && relation.getType() == TableRelationType.ONE_TO_MANY) {
                    return true;
                }
            }
        }
        return false;
    }

    public String generateWhereSql(String mainTable, TableColumnInfo tableColumnInfo, boolean needAlias, List<Object> params) {
        if (query == null) {
            return "";
        }

        String where = query.generateSql(mainTable, tableColumnInfo, params, needAlias);
        return where.isEmpty() ? "" : (" WHERE " + where);
    }

    public String generateOrderSql(String mainTable, boolean needAlias, TableColumnInfo tableColumnInfo) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getUseColumn(needAlias, entry.getKey(), mainTable, tableColumnInfo) + desc);
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
