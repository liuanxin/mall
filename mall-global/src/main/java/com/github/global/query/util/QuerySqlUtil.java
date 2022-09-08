package com.github.global.query.util;

import com.github.global.query.model.ReqParam;
import com.github.global.query.model.ReqResult;
import com.github.global.query.model.SchemaColumnInfo;

import java.util.List;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static String toFromWhereSql(SchemaColumnInfo columnInfo, String mainSchema,
                                        ReqParam param, List<Object> params) {
        StringBuilder sbd = new StringBuilder();
        // todo
        return sbd.toString();
    }

    public static String toCountSql(SchemaColumnInfo columnInfo, String fromAndWhere,
                                    String mainSchema, ReqResult result) {
        String group = result.generateGroupSql() + result.generateHavingSql();
        if (group.isEmpty()) {
            return "SELECT COUNT(*) " + fromAndWhere;
        } else {
            return "SELECT COUNT(*) FROM ( " + toSelectSql(columnInfo, fromAndWhere, mainSchema, result) + " ) TMP";
        }
    }

    private static String toSelectSql(SchemaColumnInfo columnInfo, String fromAndWhere,
                                     String mainSchema, ReqResult result) {
        return "SELECT"
                + result.generateSelectSql(mainSchema, columnInfo)
                + fromAndWhere
                + result.generateGroupSql()
                + result.generateHavingSql();
    }

    public static String toListSql(SchemaColumnInfo columnInfo, String fromAndWhere,
                                   String mainSchema, ReqParam param, ReqResult result) {
        String select = toSelectSql(columnInfo, fromAndWhere, mainSchema, result);
        return select + param.generateOrderSql();
    }

    public static String pageSql(SchemaColumnInfo columnInfo, String fromAndWhere, String mainSchema,
                                 ReqParam param, ReqResult result, List<Object> params) {
        String list = toListSql(columnInfo, fromAndWhere, mainSchema, param, result);
        return list + param.generatePageSql(params);
    }

    public static String objSql(SchemaColumnInfo columnInfo, String fromAndWhere, String mainSchema,
                                 ReqParam param, ReqResult result, List<Object> params) {
        String list = toListSql(columnInfo, fromAndWhere, mainSchema, param, result);
        return list + param.generateArrToObjSql(params);
    }
}
