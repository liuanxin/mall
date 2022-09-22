package com.github.global.query.model;

import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.JoinType;
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
 *   "noCount": true  -- true 表示不发起 SELECT COUNT(*) 查询(移动端瀑布流时有用), 不设置则默认是 false
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
    /** 分页信息 [ 当前页, 每页行数 ], 每页行数在「10, 20, 50, 100, 200, 500, 1000」, 省略则默认是 10 */
    private List<Integer> page;
    /** 当上面的分页信息有值且当前值是 true 时表示不发起 SELECT COUNT(*) 查询 */
    private Boolean noCount;


    public Set<String> checkParam(String mainSchema, SchemaColumnInfo scInfo) {
        Set<String> paramSchemaSet = new LinkedHashSet<>();
        if (query != null) {
            paramSchemaSet.addAll(query.checkCondition(mainSchema, scInfo));
        }

        if (sort != null && !sort.isEmpty()) {
            for (String column : sort.keySet()) {
                String schemaName = QueryUtil.getSchemaName(column, mainSchema);
                Schema schema = scInfo.findSchema(schemaName);
                if (schema == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined schema");
                }
                if (scInfo.findSchemaColumn(schema, QueryUtil.getColumnName(column)) == null) {
                    throw new RuntimeException("param sort(" + column + ") has no defined column");
                }
                paramSchemaSet.add(schema.getName());
            }
        }

        if (needQueryPage()) {
            Integer indexParam = page.get(0);
            if (indexParam == null || indexParam <= 0) {
                throw new RuntimeException("param page error");
            }
        }

        if (relation != null && !relation.isEmpty()) {
            Set<String> schemaRelation = new HashSet<>();
            for (List<String> values : relation) {
                if (values.size() < 3) {
                    throw new RuntimeException("param relation error");
                }
                JoinType joinType = JoinType.deserializer(values.get(1));
                if (joinType == null) {
                    throw new RuntimeException("param relation join type error");
                }
                String masterSchema = values.get(0);
                String childSchema = values.get(2);
                if (scInfo.findRelationByMasterChild(masterSchema, childSchema) == null) {
                    throw new RuntimeException("param relation " + masterSchema + " and " + childSchema + " has no relation");
                }

                String key = masterSchema + "." + childSchema;
                if (schemaRelation.contains(key)) {
                    throw new RuntimeException("param relation " + masterSchema + " and " + childSchema + " can only has one relation");
                }
                schemaRelation.add(key);
            }
            boolean hasMain = false;
            for (String schema : schemaRelation) {
                if (schema.startsWith(mainSchema + ".")) {
                    hasMain = true;
                    break;
                }
            }
            if (!hasMain) {
                throw new RuntimeException("param relation has no " + mainSchema + "'s info");
            }
        }
        return paramSchemaSet;
    }

    public List<SchemaJoinRelation> allRelationList(SchemaColumnInfo scInfo, String mainSchema) {
        Map<String, Set<SchemaJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Schema masterSchema = scInfo.findSchema(values.get(0));
                Schema childSchema = scInfo.findSchema(values.get(2));
                JoinType joinType = JoinType.deserializer(values.get(1));
                SchemaJoinRelation joinRelation = new SchemaJoinRelation(masterSchema, joinType, childSchema);
                Set<SchemaJoinRelation> relationSet = relationMap.getOrDefault(masterSchema.getName(), new LinkedHashSet<>());
                relationSet.add(joinRelation);
                relationMap.put(masterSchema.getName(), relationSet);
            }
        }
        return handleRelation(mainSchema, relationMap);
    }
    public List<SchemaJoinRelation> paramRelationList(SchemaColumnInfo scInfo, String mainSchema,
                                                      Set<String> paramSchemaSet, Set<String> resultFunctionSchemaSet) {
        Map<String, Set<SchemaJoinRelation>> relationMap = new HashMap<>();
        if (relation != null && !relation.isEmpty()) {
            for (List<String> values : relation) {
                Schema masterSchema = scInfo.findSchema(values.get(0));
                Schema childSchema = scInfo.findSchema(values.get(2));
                String mn = masterSchema.getName();
                String cn = childSchema.getName();
                if ((paramSchemaSet.contains(mn) && paramSchemaSet.contains(cn))
                        || (resultFunctionSchemaSet.contains(mn) && resultFunctionSchemaSet.contains(cn))) {
                    Set<SchemaJoinRelation> relationSet = relationMap.getOrDefault(masterSchema.getName(), new LinkedHashSet<>());
                    JoinType joinType = JoinType.deserializer(values.get(1));
                    SchemaJoinRelation joinRelation = new SchemaJoinRelation(masterSchema, joinType, childSchema);
                    relationSet.add(joinRelation);
                    relationMap.put(masterSchema.getName(), relationSet);
                }
            }
        }
        return handleRelation(mainSchema, relationMap);
    }
    private List<SchemaJoinRelation> handleRelation(String mainSchema, Map<String, Set<SchemaJoinRelation>> relationMap) {
        List<SchemaJoinRelation> relationList = new ArrayList<>();
        Set<String> relationSet = new HashSet<>();
        Set<SchemaJoinRelation> mainSet = relationMap.remove(mainSchema);
        if (mainSet != null && !mainSet.isEmpty()) {
            for (SchemaJoinRelation relation : mainSet) {
                relationList.add(relation);
                relationSet.add(relation.getMasterSchema().getName());
                relationSet.add(relation.getChildSchema().getName());
            }
        }
        for (int i = 0; i < relationMap.size(); i++) {
            for (Map.Entry<String, Set<SchemaJoinRelation>> entry : relationMap.entrySet()) {
                if (relationSet.contains(entry.getKey())) {
                    for (SchemaJoinRelation relation : entry.getValue()) {
                        relationList.add(relation);
                        relationSet.add(relation.getMasterSchema().getName());
                        relationSet.add(relation.getChildSchema().getName());
                    }
                }
            }
        }
        return relationList;
    }

    public boolean hasManyRelation(SchemaColumnInfo scInfo) {
        if (relation == null || relation.isEmpty()) {
            return false;
        }

        for (List<String> values : relation) {
            JoinType joinType = JoinType.deserializer(values.get(1));
            if (joinType != null) {
                String masterSchemaName = values.get(0);
                String childSchemaName = values.get(2);
                SchemaColumnRelation relation = scInfo.findRelationByMasterChild(masterSchemaName, childSchemaName);
                if (relation != null && relation.getType().hasMany()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String generateWhereSql(String mainSchema, SchemaColumnInfo scInfo, boolean needAlias, List<Object> params) {
        if (query == null) {
            return "";
        } else {
            String where = query.generateSql(mainSchema, scInfo, needAlias, params);
            return where.isEmpty() ? "" : (" WHERE " + where);
        }
    }

    public String generateOrderSql(String mainSchema, boolean needAlias, SchemaColumnInfo scInfo) {
        if (sort != null && !sort.isEmpty()) {
            StringJoiner orderSj = new StringJoiner(", ");
            for (Map.Entry<String, String> entry : sort.entrySet()) {
                String value = entry.getValue().toLowerCase();
                String desc = ("asc".equals(value) || "a".equals(value)) ? "" : " DESC";
                orderSj.add(QueryUtil.getUseColumn(needAlias, entry.getKey(), mainSchema, scInfo) + desc);
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
        return noCount == null || !noCount;
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
