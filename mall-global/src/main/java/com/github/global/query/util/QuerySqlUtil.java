package com.github.global.query.util;

import com.github.global.query.model.ReqParam;
import com.github.global.query.model.ReqResult;
import com.github.global.query.model.SchemaColumnInfo;

import java.util.List;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static String toFromWhereSql(SchemaColumnInfo schemaColumnInfo, String mainSchema,
                                        ReqParam param, ReqResult result, List<Object> params) {
        StringBuilder sbd = new StringBuilder();
        // todo
        return sbd.toString();
    }

    public static String toCountSql(String fromAndWhere) {
        return "SELECT COUNT(*) " + fromAndWhere;
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                     ReqParam param, ReqResult result, List<Object> params) {
        return "SELECT"
                + result.generateSelectSql(mainSchema, param.allParamSchema(mainSchema), schemaColumnInfo)
                + fromAndWhere
                + result.generateGroupSql()
                + result.generateHavingSql(params);
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
