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

    public static String toFromSql(SchemaColumnInfo schemaColumnInfo, String mainSchema, ReqParam param) {
        StringBuilder sbd = new StringBuilder("FROM ");
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        sbd.append(toSqlField(schema.getName()));

        boolean needAlias = param.needAlias(mainSchema);
        if (needAlias) {
            String mainSchemaAlias = schema.getAlias();
            sbd.append(" AS ").append(toSqlField(mainSchemaAlias));
            Set<String> paramSchema = param.allParamSchema(mainSchema);
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
                                        ReqParam param, List<Object> params) {
        boolean needAlias = param.needAlias(mainSchema);
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, param);
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
                                          ReqParam param, ReqResult result, List<Object> params,
                                          Map<String, String> functionAliasMap) {
        boolean needAlias = param.needAlias(mainSchema);
        Set<String> paramSchemaSet = param.allParamSchema(mainSchema);
        String selectField = result.generateSelectSql(mainSchema, needAlias, paramSchemaSet, schemaColumnInfo);
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

    private static boolean hasRelationMany(SchemaColumnInfo schemaColumnInfo, String mainSchema, ReqParam param) {
        Set<String> paramSchema = param.allParamSchema(mainSchema);
        paramSchema.remove(mainSchema);
        for (String childSchemaName : paramSchema) {
            SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(mainSchema, childSchemaName);
            if (relation != null && relation.getType() == SchemaRelationType.ONE_TO_MANY) {
                return true;
            }
        }
        return false;
    }
    public static String toCountWithoutGroupSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                                ReqParam param, String fromAndWhere) {
        if (hasRelationMany(schemaColumnInfo, mainSchema, param)) {
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

    public static String toPageWithoutGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                               ReqParam param, ReqResult result, List<Object> params) {
        boolean needAlias = param.needAlias(mainSchema);
        Set<String> paramSchemaSet = param.allParamSchema(mainSchema);
        String selectField = result.generateSelectSql(mainSchema, needAlias, paramSchemaSet, schemaColumnInfo);
        // SELECT ... FROM ... WHERE ... ORDER BY ..
        return "SELECT " + selectField + fromAndWhere + param.generatePageSql(params);
    }

    public static String toIdPageSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                     ReqParam param, List<Object> params) {
        StringJoiner sj = new StringJoiner(", ");
        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        boolean needAlias = param.needAlias(mainSchema);
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
    public static String toSelectSqlWithId(SchemaColumnInfo schemaColumnInfo, String mainSchema, ReqParam param,
                                           ReqResult result, List<Map<String, Object>> idList, List<Object> idFromParams) {
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
        boolean needAlias = param.needAlias(mainSchema);
        Set<String> paramSchemaSet = param.allParamSchema(mainSchema);
        String selectColumn = result.generateSelectSql(mainSchema, needAlias, paramSchemaSet, schemaColumnInfo);
        String fromSql = toFromSql(schemaColumnInfo, mainSchema, param);
        String idKeyColumn = schema.idKeyColumn(needAlias, schema.getAlias());
        return String.format("SELECT %s FROM %s WHERE %s IN %s", selectColumn, fromSql, idKeyColumn, sj);
    }

    public static String toPageGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                        ReqParam param, ReqResult result, List<Object> params,
                                        Map<String, String> functionAliasMap) {
        String listSql = toListGroupSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params, functionAliasMap);
        return listSql + param.generatePageSql(params);
    }

    public static String toListGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                        ReqParam param, ReqResult result, List<Object> params,
                                        Map<String, String> functionAliasMap) {
        String selectSql = toGroupSelectSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params, functionAliasMap);
        String orderSql = param.generateOrderSql(mainSchema, param.needAlias(mainSchema), schemaColumnInfo);
        return selectSql + orderSql;
    }

    public static String toObjGroupSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                       ReqParam param, ReqResult result, List<Object> params,
                                       Map<String, String> functionAliasMap) {
        String listSql = toListGroupSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params, functionAliasMap);
        return listSql + param.generateArrToObjSql(params);
    }
}
