package com.github.global.query.util;

import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.*;

import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static String toFromWhereSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                        ReqParam param, boolean needAlias, List<Object> params) {
        StringBuilder sbd = new StringBuilder();
        sbd.append("FROM ");

        Schema schema = schemaColumnInfo.findSchema(mainSchema);
        sbd.append(QuerySqlUtil.toSqlField(schema.getName()));

        if (needAlias) {
            String mainSchemaAlias = schema.getAlias();
            sbd.append(" AS ").append(QuerySqlUtil.toSqlField(mainSchemaAlias));
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

                sbd.append(" INNER JOIN ").append(QuerySqlUtil.toSqlField(childSchemaColumn.getName()));
                sbd.append(" AS ").append(QuerySqlUtil.toSqlField(childAlias));
                sbd.append(" ON ").append(QuerySqlUtil.toSqlField(mainSchemaAlias)).append(".");
                sbd.append(QuerySqlUtil.toSqlField(relation.getOneColumn()));
                sbd.append(" = ").append(QuerySqlUtil.toSqlField(childAlias)).append(".");
                sbd.append(QuerySqlUtil.toSqlField(relation.getOneOrManyColumn()));
            }
        }
        return sbd.append(param.generateWhereSql(mainSchema, schemaColumnInfo, params, needAlias)).toString();
    }

    public static String toCountSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                    ReqParam param, String fromAndWhere) {
        // SELECT COUNT(DISTINCT xx.id) FROM ...
        if (hasRelationMany(schemaColumnInfo, mainSchema, param)) {
            StringJoiner sj = new StringJoiner(", ");
            for (String id : schemaColumnInfo.getSchemaMap().get(mainSchema).getIdKey()) {
                sj.add(QuerySqlUtil.toSqlField(mainSchema) + "." + QuerySqlUtil.toSqlField(id));
            }
            return String.format("SELECT COUNT(DISTINCT %s) ", sj) + fromAndWhere;
        }
        return "SELECT COUNT(*) " + fromAndWhere;
    }
    private static boolean hasRelationMany(SchemaColumnInfo schemaColumnInfo, String mainSchema, ReqParam param) {
        Set<String> paramSchema = param.allParamSchema(mainSchema);
        paramSchema.remove(mainSchema);
        for (String childSchemaName : paramSchema) {
            SchemaColumnRelation relation = schemaColumnInfo.findRelationByMasterChild(mainSchema, childSchemaName);
            if (relation.getType() == SchemaRelationType.ONE_TO_MANY) {
                return true;
            }
        }
        return false;
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                     ReqParam param, ReqResult result, List<Object> params) {
        String selectField = result.generateSelectSql(mainSchema, param.allParamSchema(mainSchema), schemaColumnInfo);
        String functionSql = result.generateSelectFunctionSql(mainSchema, schemaColumnInfo);
        boolean emptySelect = selectField.isEmpty();
        boolean emptyFunction = functionSql.isEmpty();
        if (emptySelect && emptyFunction) {
            throw new RuntimeException("generate select sql error: no select field");
        }

        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }

        if (!emptyFunction) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql());
        sbd.append(result.generateHavingSql(params));
        return sbd.toString();
    }

    public static String toPageSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                   ReqParam param, ReqResult result, List<Object> params) {
        String listSql = toListSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        return listSql + param.generatePageSql(params);
    }

    public static String toListSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                   ReqParam param, ReqResult result, List<Object> params) {
        String selectSql = toSelectSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        return selectSql + param.generateOrderSql();
    }

    public static String toObjSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                  ReqParam param, ReqResult result, List<Object> params) {
        String listSql = toListSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        return listSql + param.generateArrToObjSql(params);
    }
}
