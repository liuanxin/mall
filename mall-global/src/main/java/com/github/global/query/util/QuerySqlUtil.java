package com.github.global.query.util;

import com.github.global.query.model.*;

import java.util.*;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    private static String toFromSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                    Map<String, Set<SchemaJoinRelation>> joinRelationMap) {
        StringBuilder sbd = new StringBuilder("FROM ");
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        sbd.append(toSqlField(schema.getName()));

        Map<String, Set<SchemaJoinRelation>> tempMap = new LinkedHashMap<>(joinRelationMap);
        if (!tempMap.isEmpty()) {
            Set<SchemaJoinRelation> masterRelation = tempMap.remove(schema.getName());
            if (!masterRelation.isEmpty()) {
                for (SchemaJoinRelation joinRelation : masterRelation) {
                    sbd.append(joinRelation.generateJoin(schemaColumnInfo));
                }

                for (Set<SchemaJoinRelation> relations : tempMap.values()) {
                    for (SchemaJoinRelation joinRelation : relations) {
                        sbd.append(joinRelation.generateJoin(schemaColumnInfo));
                    }
                }
            }
        }
        return sbd.toString();
    }

    public static String toFromWhereSql(SchemaColumnInfo schemaColumnInfo, String mainSchema, boolean needAlias,
                                        ReqParam param, Map<String, Set<SchemaJoinRelation>> joinRelationMap,
                                        List<Object> params) {
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, joinRelationMap);
        String whereSql = param.generateWhereSql(mainSchema, schemaColumnInfo, params, needAlias);
        return fromSql + whereSql;
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                          Set<String> paramSchemaSet, ReqResult result, List<Object> params) {
        String selectField = result.generateSelectSql(mainSchema, paramSchemaSet, schemaColumnInfo);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        boolean needAlias = !paramSchemaSet.isEmpty();
        String functionSql = result.generateFunctionSql(mainSchema, needAlias, schemaColumnInfo);
        if (!functionSql.isEmpty()) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql(mainSchema, needAlias, schemaColumnInfo));
        sbd.append(result.generateHavingSql(mainSchema, needAlias, schemaColumnInfo, params));
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                                boolean needAlias, ReqParam param, String fromAndWhere) {
        if (param.hasManyRelation(schemaColumnInfo)) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String idSelect = schemaColumnInfo.findSchema(mainSchema).idSelect(needAlias);
            return String.format("SELECT COUNT(DISTINCT %s) %s", idSelect, fromAndWhere);
        } else {
            return "SELECT COUNT(*) " + fromAndWhere;
        }
    }

    public static String toPageWithoutGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere,
                                               String mainSchema, Set<String> paramSchemaSet,
                                               ReqParam param, ReqResult result, List<Object> params) {
        String selectField = result.generateSelectSql(mainSchema, paramSchemaSet, schemaColumnInfo);
        // SELECT ... FROM ... WHERE ... ORDER BY ..
        return "SELECT " + selectField + fromAndWhere + param.generatePageSql(params);
    }

    public static String toIdPageSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                     boolean needAlias, ReqParam param, List<Object> params) {
        String idSelect = schemaColumnInfo.findSchema(mainSchema).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainSchema, needAlias, schemaColumnInfo);
        return "SELECT " + idSelect + fromAndWhere + orderSql + param.generatePageSql(params);
    }
    public static String toSelectWithIdSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                           Set<String> paramSchemaSet, ReqResult result,
                                           List<Map<String, Object>> idList,
                                           Map<String, Set<SchemaJoinRelation>> joinRelationMap,
                                           List<Object> params) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateSelectSql(mainSchema, paramSchemaSet, schemaColumnInfo);
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, joinRelationMap);

        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        String idWhere = schema.idWhere(!paramSchemaSet.isEmpty());
        List<String> idKey = schema.getIdKey();
        StringJoiner sj = new StringJoiner(", ", "( ", " )");
        for (Map<String, Object> idMap : idList) {
            if (idKey.size() > 1) {
                // WHERE (id1, id2) IN ( (X, XX), (Y, YY) )
                StringJoiner innerJoiner = new StringJoiner(", ", "(", ")");
                for (String id : idKey) {
                    innerJoiner.add("?");
                    params.add(idMap.get(id));
                }
                sj.add(innerJoiner.toString());
            } else {
                // WHERE id IN (x, y, z)
                sj.add("?");
                params.add(idMap.get(idKey.get(0)));
            }
        }
        return String.format("SELECT %s FROM %s WHERE %s IN %s", selectColumn, fromSql, idWhere, sj);
    }
}
