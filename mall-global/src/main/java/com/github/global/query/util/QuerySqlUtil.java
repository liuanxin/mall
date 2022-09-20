package com.github.global.query.util;

import com.github.global.query.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class QuerySqlUtil {

    public static String toSqlField(String field) {
        return MysqlKeyWordUtil.hasKeyWord(field) ? ("`" + field + "`") : field;
    }

    public static String toFromSql(TableColumnInfo tableColumnInfo, String mainTable,
                                   List<TableJoinRelation> joinRelationList) {
        StringBuilder sbd = new StringBuilder("FROM ");
        Table table = tableColumnInfo.findTable(mainTable);
        String mainTableName = table.getName();
        sbd.append(toSqlField(mainTableName));
        if (joinRelationList != null && !joinRelationList.isEmpty()) {
            sbd.append(" AS ").append(table.getAlias());
            for (TableJoinRelation joinRelation : joinRelationList) {
                sbd.append(joinRelation.generateJoin(tableColumnInfo));
            }
        }
        return sbd.toString();
    }

    public static String toWhereSql(TableColumnInfo tableColumnInfo, String mainTable,
                                    boolean needAlias, ReqParam param, List<Object> params) {
        return param.generateWhereSql(mainTable, tableColumnInfo, needAlias, params);
    }

    public static String toCountGroupSql(String selectSql) {
        return "SELECT COUNT(*) FROM ( " + selectSql + " ) TMP";
    }

    public static String toSelectGroupSql(TableColumnInfo tableColumnInfo, String fromAndWhere, String mainTable,
                                          boolean needAlias, ReqResult result,
                                          Set<String> firstQueryTableSet, List<Object> params) {
        String selectField = result.generateSelectSql(mainTable, needAlias, tableColumnInfo, firstQueryTableSet);
        boolean emptySelect = selectField.isEmpty();

        // SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ...
        StringBuilder sbd = new StringBuilder("SELECT ");
        if (!emptySelect) {
            sbd.append(selectField);
        }
        String functionSql = result.generateFunctionSql(mainTable, needAlias, tableColumnInfo);
        if (!functionSql.isEmpty()) {
            if (!emptySelect) {
                sbd.append(", ");
            }
            sbd.append(functionSql);
        }
        sbd.append(fromAndWhere);
        sbd.append(result.generateGroupSql(mainTable, needAlias, tableColumnInfo));
        sbd.append(result.generateHavingSql(mainTable, needAlias, tableColumnInfo, params));
        return sbd.toString();
    }

    public static String toCountWithoutGroupSql(TableColumnInfo tableColumnInfo, String mainTable,
                                                boolean needAlias, ReqParam param, String fromAndWhere) {
        if (param.hasManyRelation(tableColumnInfo)) {
            // SELECT COUNT(DISTINCT xx.id) FROM ...
            String idSelect = tableColumnInfo.findTable(mainTable).idSelect(needAlias);
            return String.format("SELECT COUNT(DISTINCT %s) %s", idSelect, fromAndWhere);
        } else {
            return "SELECT COUNT(*) " + fromAndWhere;
        }
    }

    public static String toPageWithoutGroupSql(TableColumnInfo tableColumnInfo, String fromAndWhere, String mainTable,
                                               boolean needAlias, ReqParam param, ReqResult result,
                                               Set<String> firstQueryTableSet, List<Object> params) {
        String selectField = result.generateSelectSql(mainTable, needAlias, tableColumnInfo, firstQueryTableSet);
        // SELECT ... FROM ... WHERE ... ORDER BY ..
        return "SELECT " + selectField + fromAndWhere + param.generatePageSql(params);
    }

    public static String toIdPageSql(TableColumnInfo tableColumnInfo, String fromAndWhere, String mainTable,
                                     boolean needAlias, ReqParam param, List<Object> params) {
        String idSelect = tableColumnInfo.findTable(mainTable).idSelect(needAlias);
        // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
        String orderSql = param.generateOrderSql(mainTable, needAlias, tableColumnInfo);
        return "SELECT " + idSelect + fromAndWhere + orderSql + param.generatePageSql(params);
    }
    public static String toSelectWithIdSql(TableColumnInfo tableColumnInfo, String mainTable, String fromSql,
                                           boolean needAlias, ReqResult result, List<Map<String, Object>> idList,
                                           Set<String> firstQueryTableSet, List<Object> params) {
        // SELECT ... FROM ... WHERE id IN (x, y, z)
        String selectColumn = result.generateSelectSql(mainTable, needAlias, tableColumnInfo, firstQueryTableSet);

        Table table = tableColumnInfo.findTable(mainTable);
        String idWhere = table.idWhere(needAlias);
        List<String> idKey = table.getIdKey();
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
