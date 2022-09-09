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
                                        ReqParam param, List<Object> params) {
        StringBuilder sbd = new StringBuilder();
        // todo
        return sbd.toString();
    }

    public static String toCountSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere,
                                    String mainSchema, ReqResult result) {
        String group = result.generateGroupSql() + result.generateHavingSql();
        if (group.isEmpty()) {
            return "SELECT COUNT(*) " + fromAndWhere;
        } else {
            return "SELECT COUNT(*) FROM ( " + toSelectSql(schemaColumnInfo, fromAndWhere, mainSchema, result) + " ) TMP";
        }
    }

    private static String toSelectSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere,
                                     String mainSchema, ReqResult result) {
        return "SELECT"
                + result.generateSelectSql(mainSchema, schemaColumnInfo)
                + fromAndWhere
                + result.generateGroupSql()
                + result.generateHavingSql();
    }

    public static String toListSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere,
                                   String mainSchema, ReqParam param, ReqResult result) {
        String select = toSelectSql(schemaColumnInfo, fromAndWhere, mainSchema, result);
        return select + param.generateOrderSql();
    }

    public static String toPageSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                   ReqParam param, ReqResult result, List<Object> params) {
        String list = toListSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result);
        return list + param.generatePageSql(params);
    }

    public static String toObjSql(SchemaColumnInfo schemaColumnInfo, String fromAndWhere, String mainSchema,
                                  ReqParam param, ReqResult result, List<Object> params) {
        String list = toListSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result);
        return list + param.generateArrToObjSql(params);
    }
}
