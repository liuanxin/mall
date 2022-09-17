package com.github.global.query.util;

import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

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
            paramSchema.remove(mainSchema);
            for (String childSchemaName : paramSchema) {
                SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(mainSchema, childSchemaName);
                if (relation == null) {
                    throw new RuntimeException(mainSchema + " - " + childSchemaName + " has no relation");
                }
                String childColumn = relation.getOneOrManyColumn();

                SchemaColumn childSchemaColumn = schemaColumnInfo.findSchemaColumn(childSchemaName, childColumn);
                String childAlias = childSchemaColumn.getAlias();

                sbd.append(" INNER JOIN ").append(toSqlField(childSchemaColumn.getName()));
                sbd.append(" AS ").append(toSqlField(childAlias));
                sbd.append(" ON ").append(toSqlField(mainSchemaAlias)).append(".");
                sbd.append(toSqlField(relation.getOneColumn()));
                sbd.append(" = ").append(toSqlField(childAlias)).append(".");
                sbd.append(toSqlField(relation.getOneOrManyColumn()));
            }
        }
        return sbd.toString();
    }

    public static String toFromWhereSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                        Set<String> paramSchema, ReqParam param, List<Object> params) {
        boolean needAlias = !paramSchema.isEmpty();
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, paramSchema);
        String whereSql = param.generateWhereSql(mainSchema, schemaColumnInfo, params, needAlias);
        return fromSql + whereSql;
    }

    public static String toGroupCountSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toGroupListSql(String selectSql, ReqParam param, List<Object> params) {
        return selectSql + param.generatePageSql(params);
    }

    public static String toGroupSelectSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                          Set<String> paramSchema, ReqResult result,
                                          List<Object> params, Map<String, String> functionAliasMap) {
        boolean needAlias = !paramSchema.isEmpty();
        String selectField = result.generateSelectSql(mainSchema, paramSchema, schemaColumnInfo);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        String functionSql = result.generateFunctionSql(mainSchema, needAlias, schemaColumnInfo, functionAliasMap);
        if (!functionSql.isEmpty()) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql(mainSchema, needAlias, schemaColumnInfo));
        sbd.append(result.generateHavingSql(params));
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
            StringJoiner sj = new StringJoiner(", ");
            Schema schema = schemaColumnInfo.findSchema(mainSchema);
            for (String id : schema.getIdKey()) {
                sj.add(toSqlField(schema.getAlias()) + "." + toSqlField(id));
            }
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            return String.format("SELECT COUNT(DISTINCT %s) ", sj) + fromAndWhere;
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
        StringJoiner sj = new StringJoiner(", ");
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        for (String id : schema.getIdKey()) {
            if (needAlias) {
                sj.add(toSqlField(schema.getAlias()) + "." + toSqlField(id));
            } else {
                sj.add(toSqlField(id));
            }
        }
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainSchema, needAlias, schemaColumnInfo);
        return "SELECT " + sj + fromAndWhere + orderSql + param.generatePageSql(params);
    }
    public static String toSelectWithIdSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                           Set<String> paramSchema, ReqResult result,
                                           List<Map<String, Object>> idList, List<Object> idFromParams) {
        boolean needAlias = !paramSchema.isEmpty();
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        List<String> idKey = schema.getIdKey();
        StringJoiner sj = new StringJoiner(", ", "( ", " )");
        for (Map<String, Object> idMap : idList) {
            if (idKey.size() > 1) {
                // WHERE (id1, id2) IN ( ( X, XX ), ( Y, YY ) )
                StringJoiner innerJoiner = new StringJoiner(", ", "(", ")");
                for (String ik : idKey) {
                    innerJoiner.add("?");
                    idFromParams.add(idMap.get(ik));
                }
                sj.add(innerJoiner.toString());
            } else {
                // ... WHERE id IN (x, y, z)
                sj.add("?");
                idFromParams.add(idMap.get(idKey.get(0)));
            }
        }
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateSelectSql(mainSchema, paramSchema, schemaColumnInfo);
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, paramSchema);
        String idKeyColumn = schema.idKeyColumn(needAlias, schema.getAlias());
        return String.format("SELECT %s FROM %s WHERE %s IN %s", selectColumn, fromSql, idKeyColumn, sj);
    }
}
