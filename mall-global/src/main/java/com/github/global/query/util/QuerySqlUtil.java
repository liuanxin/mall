package com.github.global.query.util;

import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.*;

import java.util.*;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    private static String toFromSql(SchemaColumnInfo schemaColumnInfo, String mainSchema, Set<String> paramSchema) {
        StringBuilder sbd = new StringBuilder("FROM ");
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        sbd.append(toSqlField(schema.getName()));

        if (!paramSchema.isEmpty()) {
            String mainSchemaAlias = schema.getAlias();
            sbd.append(" AS ").append(toSqlField(mainSchemaAlias));
            for (String childSchemaName : paramSchema) {
                SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(mainSchema, childSchemaName);
                SchemaColumnRelation useRelation = (relation == null) ?
                        findRelation(schemaColumnInfo, childSchemaName, paramSchema) : relation;
                if (useRelation == null) {
                    throw new RuntimeException(childSchemaName + " has no relation with other schemas");
                }

                String childColumn = useRelation.getOneOrManyColumn();
                SchemaColumn childSchemaColumn = schemaColumnInfo.findSchemaColumn(childSchemaName, childColumn);
                String childAlias = childSchemaColumn.getAlias();

                sbd.append(" INNER JOIN ").append(toSqlField(childSchemaColumn.getName()));
                sbd.append(" AS ").append(toSqlField(childAlias));
                sbd.append(" ON ").append(toSqlField(mainSchemaAlias)).append(".");
                sbd.append(toSqlField(useRelation.getOneColumn()));
                sbd.append(" = ").append(toSqlField(childAlias)).append(".");
                sbd.append(toSqlField(useRelation.getOneOrManyColumn()));
            }
        }
        return sbd.toString();
    }
    private static SchemaColumnRelation findRelation(SchemaColumnInfo schemaColumnInfo,
                                                     String childSchemaName, Set<String> paramSchema) {
        Set<String> tempSchemaSet = new LinkedHashSet<>(paramSchema);
        tempSchemaSet.remove(childSchemaName);
        for (String tempSchema : tempSchemaSet) {
            SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(tempSchema, childSchemaName);
            if (relation != null) {
                return relation;
            }
        }
        return null;
    }

    public static String toFromWhereSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                        Set<String> paramSchema, ReqParam param, List<Object> params) {
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, paramSchema);
        String whereSql = param.generateWhereSql(mainSchema, schemaColumnInfo, params, !paramSchema.isEmpty());
        return fromSql + whereSql;
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                          Set<String> paramSchema, ReqResult result, List<Object> params) {
        String selectField = result.generateSelectSql(mainSchema, paramSchema, schemaColumnInfo);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        boolean needAlias = !paramSchema.isEmpty();
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

    private static boolean hasRelationMany(SchemaColumnInfo schemaColumnInfo, String mainSchema, Set<String> paramSchema) {
        for (String childSchemaName : paramSchema) {
            SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(mainSchema, childSchemaName);
            if (relation != null && relation.getType() == SchemaRelationType.ONE_TO_MANY) {
                return true;
            }
        }
        return false;
    }
    public static String toCountWithoutGroupSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                                Set<String> paramSchema, String fromAndWhere) {
        if (hasRelationMany(schemaColumnInfo, mainSchema, paramSchema)) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String idSelect = schemaColumnInfo.findSchema(mainSchema).idSelect(!paramSchema.isEmpty());
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
                                           Set<String> paramSchema, ReqResult result,
                                           List<Map<String, Object>> idList, List<Object> params) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateSelectSql(mainSchema, paramSchema, schemaColumnInfo);
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, paramSchema);

        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        String idWhere = schema.idWhere(!paramSchema.isEmpty());
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
