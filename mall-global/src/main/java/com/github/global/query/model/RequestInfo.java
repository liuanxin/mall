package com.github.global.query.model;

import com.github.global.query.enums.JoinType;
import com.github.global.query.enums.ResultType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    /** 主表 */
    private String schema;
    /** 入参 */
    private ReqParam param;
    /** 出参类型, 对象(obj)还是数组(arr), 不设置则是数组 */
    private ResultType type;
    /** 出参 */
    private ReqResult result;

    /** { [ "order", "inner", "orderAddress" ] , [ "order", "left", "orderItem" ] , [ "order", "right", "orderPrice" ] ] */
    private List<List<String>> relation;


    public void checkSchema(SchemaColumnInfo scInfo) {
        if (schema == null || schema.isEmpty()) {
            throw new RuntimeException("request need schema");
        }
        if (scInfo.findSchema(schema) == null) {
            throw new RuntimeException("request has no defined schema(" + schema + ")");
        }
    }

    public Set<String> checkParam(SchemaColumnInfo scInfo) {
        if (param == null) {
            throw new RuntimeException("request need param");
        }
        return param.checkParam(schema, scInfo);
    }

    public Set<String> checkResult(SchemaColumnInfo scInfo, Set<String> allResultSchema) {
        if (result == null) {
            throw new RuntimeException("request need result");
        }
        return result.checkResult(schema, scInfo, allResultSchema);
    }

    public void checkAllSchema(SchemaColumnInfo scInfo, Set<String> allSchemaSet,
                               Set<String> paramSchemaSet, Set<String> resultSchemaSet) {
        paramSchemaSet.remove(schema);
        resultSchemaSet.remove(schema);
        if (relation == null || relation.isEmpty()) {
            if (!paramSchemaSet.isEmpty() || !resultSchemaSet.isEmpty()) {
                throw new RuntimeException("request need relation");
            }
        }
        checkRelation(scInfo);

        for (String paramSchema : paramSchemaSet) {
            if (!allSchemaSet.contains(paramSchema)) {
                throw new RuntimeException("relation need param schema(" + paramSchema + ")");
            }
        }
        for (String resultSchema : resultSchemaSet) {
            if (!allSchemaSet.contains(resultSchema)) {
                throw new RuntimeException("relation need result schema(" + resultSchema + ")");
            }
        }
    }
    private void checkRelation(SchemaColumnInfo scInfo) {
        if (relation != null && !relation.isEmpty()) {
            Set<String> schemaRelation = new HashSet<>();
            for (List<String> values : relation) {
                if (values.size() < 3) {
                    throw new RuntimeException("relation error");
                }
                JoinType joinType = JoinType.deserializer(values.get(1));
                if (joinType == null) {
                    throw new RuntimeException("relation join type error");
                }
                String masterSchema = values.get(0);
                String childSchema = values.get(2);
                if (scInfo.findRelationByMasterChild(masterSchema, childSchema) == null) {
                    throw new RuntimeException("relation " + masterSchema + " and " + childSchema + " has no relation");
                }

                String key = masterSchema + "<->" + childSchema;
                if (schemaRelation.contains(key)) {
                    throw new RuntimeException("relation " + masterSchema + " and " + childSchema + " can only has one relation");
                }
                schemaRelation.add(key);
            }
            boolean hasMain = false;
            for (String schema : schemaRelation) {
                if (schema.startsWith(schema + ".")) {
                    hasMain = true;
                    break;
                }
            }
            if (!hasMain) {
                throw new RuntimeException("relation has no " + schema + "'s info");
            }
        }
    }

    public List<SchemaJoinRelation> allRelationList(SchemaColumnInfo scInfo) {
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
        return handleRelation(schema, relationMap);
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
    public List<SchemaJoinRelation> paramRelationList(SchemaColumnInfo scInfo, Set<String> paramSchemaSet,
                                                      Set<String> resultFunctionSchemaSet) {
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
        return handleRelation(schema, relationMap);
    }
}
