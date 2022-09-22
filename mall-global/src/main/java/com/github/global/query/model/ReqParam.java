package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqJoinType;
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
 *   "notCount": true  -- true 表示不发起 SELECT COUNT(*) 查询(移动端瀑布流时有用), 不设置则默认是 false
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
    /** 当上面的分页信息有值且当前值是 true 时表示不发起 SELECT COUNT(*) 查询 */
    private Boolean notCount;


    public Set<String> checkParam(String mainTable, TableColumnInfo tcInfo) {
        Set<String> paramTableSet = new LinkedHashSet<>();
        if (query != null) {
            paramTableSet.addAll(query.checkCondition(mainTable, tcInfo));
        }

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                String tableName = QueryUtil.getTableName(column, mainTable);
                Table table = tcInfo.findTable(tableName);
                if (table == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined table");
                }
                if (tcInfo.findTableColumn(table, QueryUtil.getColumnName(column)) == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined column");
                }
                paramTableSet.add(table.getName());
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
                if (values.size() < 3) {
                    throw new RuntimeException("param relation error");
                }
                ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                if (joinType == null) {
                    throw new RuntimeException("param relation join type error");
                }
                String masterTable = values.get(0);
                String childTable = values.get(2);
                if (tcInfo.findRelationByMasterChild(masterTable, childTable) == null) {
                    throw new RuntimeException("param relation " + masterTable + " and " + childTable + " has no relation");
                }

                String key = masterTable + "." + childTable;
                if (tableRelation.contains(key)) {
                    throw new RuntimeException("param relation " + masterTable + " and " + childTable + " can only has one relation");
                }
                tableRelation.add(key);
            }
            boolean hasMain = false;
            for (String table : tableRelation) {
                if (table.startsWith(mainTable + ".")) {
                    hasMain = true;
                    break;
                }
            }
            if (!hasMain) {
                throw new RuntimeException("param relation has no " + mainTable + "'s info");
            }
        }
        return paramTableSet;
    }

    public List<TableJoinRelation> allRelationList(TableColumnInfo tcInfo, String mainTable) {
        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Table masterTable = tcInfo.findTable(values.get(0));
                Table childTable = tcInfo.findTable(values.get(2));
                ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                TableJoinRelation joinRelation = new TableJoinRelation(masterTable, joinType, childTable);
                Set<TableJoinRelation> relationSet = relationMap.getOrDefault(masterTable.getName(), new LinkedHashSet<>());
                relationSet.add(joinRelation);
                relationMap.put(masterTable.getName(), relationSet);
            }
        }
        return handleRelation(mainTable, relationMap);
    }
    public List<TableJoinRelation> paramRelationList(TableColumnInfo tcInfo, String mainTable,
                                                     Set<String> paramTableSet, Set<String> resultFunctionTableSet) {
        Map<String, Set<TableJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Table masterTable = tcInfo.findTable(values.get(0));
                Table childTable = tcInfo.findTable(values.get(2));
                String mn = masterTable.getName();
                String cn = childTable.getName();
                if ((paramTableSet.contains(mn) && paramTableSet.contains(cn))
                        || (resultFunctionTableSet.contains(mn) && resultFunctionTableSet.contains(cn))) {
                    Set<TableJoinRelation> relationSet = relationMap.getOrDefault(masterTable.getName(), new LinkedHashSet<>());
                    ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
                    TableJoinRelation joinRelation = new TableJoinRelation(masterTable, joinType, childTable);
                    relationSet.add(joinRelation);
                    relationMap.put(masterTable.getName(), relationSet);
                }
            }
        }
        return handleRelation(mainTable, relationMap);
    }
    private List<TableJoinRelation> handleRelation(String mainTable, Map<String, Set<TableJoinRelation>> relationMap) {
        List<TableJoinRelation> relationList = new ArrayList<>();
        Set<String> relationSet = new HashSet<>();
        Set<TableJoinRelation> mainSet = relationMap.remove(mainTable);
        if (mainSet != null && !mainSet.isEmpty()) {
            for (TableJoinRelation relation : mainSet) {
                relationList.add(relation);
                relationSet.add(relation.getMasterTable().getName());
                relationSet.add(relation.getChildTable().getName());
            }
        }
        for (int i = 0; i < relationMap.size(); i++) {
            for (Map.Entry<String, Set<TableJoinRelation>> entry : relationMap.entrySet()) {
                if (relationSet.contains(entry.getKey())) {
                    for (TableJoinRelation relation : entry.getValue()) {
                        relationList.add(relation);
                        relationSet.add(relation.getMasterTable().getName());
                        relationSet.add(relation.getChildTable().getName());
                    }
                }
            }
        }
        return relationList;
    }

    public boolean hasManyRelation(TableColumnInfo tcInfo) {
        if (relation == null || relation.isEmpty()) {
            return false;
        }

        for (List<String> values : relation) {
            ReqJoinType joinType = ReqJoinType.deserializer(values.get(1));
            if (joinType != null) {
                String masterTableName = values.get(0);
                String childTableName = values.get(2);
                TableColumnRelation relation = tcInfo.findRelationByMasterChild(masterTableName, childTableName);
                if (relation != null && relation.getType().hasMany()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String generateWhereSql(String mainTable, TableColumnInfo tcInfo, boolean needAlias, List<Object> params) {
        if (query == null) {
            return "";
        } else {
            String where = query.generateSql(mainTable, tcInfo, needAlias, params);
            return where.isEmpty() ? "" : (" WHERE " + where);
        }
    }

    public String generateOrderSql(String mainTable, boolean needAlias, TableColumnInfo tcInfo) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getUseColumn(needAlias, entry.getKey(), mainTable, tcInfo) + desc);
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
