package com.github.global.query.config;

import com.github.common.collection.MapMultiUtil;
import com.github.common.collection.MapMultiValue;
import com.github.common.json.JsonUtil;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqResultType;
import com.github.global.query.enums.TableRelationType;
import com.github.global.query.model.*;
import com.github.global.query.util.QuerySqlUtil;
import com.github.global.query.util.QueryUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QueryTableInfoConfig {

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    private final JdbcTemplate jdbcTemplate;
    private final TableColumnInfo tcInfo;

    public QueryTableInfoConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.tcInfo = (scanPackages == null || scanPackages.isEmpty()) ? initWithDb() : QueryUtil.scanTable(scanPackages);
    }
    private TableColumnInfo initWithDb() {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Table> tableMap = new LinkedHashMap<>();
        List<TableColumnRelation> relationList = new ArrayList<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // table_name, table_comment
        List<Map<String, Object>> tableList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        // table_name, column_name, column_type, column_comment, has_pri, varchar_length
        List<Map<String, Object>> tableColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        // table_name, column_name, relation_table_name, relation_column_name (relation : one or many)
        List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
        // table_name, column_name, has_single_unique
        List<Map<String, Object>> indexList = jdbcTemplate.queryForList(QueryConst.INDEX_SQL, dbName);

        MapMultiValue<String, Map<String, Object>, List<Map<String, Object>>> tableColumnMap = MapMultiUtil.createMapList();
        if (!tableColumnList.isEmpty()) {
            for (Map<String, Object> tableColumn : tableColumnList) {
                tableColumnMap.put(QueryUtil.toStr(tableColumn.get("tn")), tableColumn);
            }
        }
        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String tableName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(tableName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(tableName, columnMap);
            }
        }
        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String tableName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(tableName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(tableName, uniqueColumnSet);
            }
        }

        for (Map<String, Object> tableInfo : tableList) {
            String tableName = QueryUtil.toStr(tableInfo.get("tn"));
            String tableAlias = QueryUtil.tableNameToAlias(tableName);
            String tableDesc = QueryUtil.toStr(tableInfo.get("tc"));
            Map<String, TableColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = tableColumnMap.get(tableName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> clazz = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToAlias(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                int strLen = QueryUtil.toInt(QueryUtil.toStr(columnInfo.get("cml")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new TableColumn(columnName, columnDesc, columnAlias, primary, strLen, clazz));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + tableName, tableAlias);
            tableMap.put(tableAlias, new Table(tableName, tableDesc, tableAlias, columnMap));
        }

        if (!relationColumnMap.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Object>>> entry : relationColumnMap.entrySet()) {
                String relationTable = entry.getKey();
                Set<String> uniqueColumnSet = columnUniqueMap.get(relationTable);
                for (Map.Entry<String, Map<String, Object>> columnEntry : entry.getValue().entrySet()) {
                    String relationColumn = columnEntry.getKey();
                    TableRelationType type = uniqueColumnSet.contains(relationColumn)
                            ? TableRelationType.ONE_TO_ONE : TableRelationType.ONE_TO_MANY;

                    Map<String, Object> relationInfoMap = columnEntry.getValue();
                    String oneTable = QueryUtil.toStr(relationInfoMap.get("ftn"));
                    String oneColumn = QueryUtil.toStr(relationInfoMap.get("fcn"));

                    relationList.add(new TableColumnRelation(oneTable, oneColumn, type, relationTable, relationColumn));
                }
            }
        }
        return new TableColumnInfo(aliasMap, tableMap, relationList);
    }


    public List<QueryInfo> queryInfo() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Table table : tcInfo.allTable()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (TableColumn column : table.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                Integer length = (column.getStrLen() == 0) ? null : column.getStrLen();
                TableColumnRelation relation = tcInfo.findRelationByChild(table.getName(), column.getName());
                String rel = (relation == null) ? null : (relation.getOneTable() + "." + relation.getOneColumn());
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type, length, rel));
            }
            queryList.add(new QueryInfo(table.getAlias(), table.getDesc(), columnList));
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        Set<String> paramTableSet = req.checkParam(tcInfo);
        Set<String> resultFuncTableSet = req.checkResult(tcInfo);

        String mainTable = req.getTable();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        List<TableJoinRelation> allRelationList = param.allRelationList(tcInfo, mainTable);
        Set<String> allTableSet = calcTableSet(allRelationList);
        String allFromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, allRelationList);

        List<Object> params = new ArrayList<>();
        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                List<TableJoinRelation> paramRelationList = param.paramRelationList(tcInfo, mainTable, paramTableSet, resultFuncTableSet);
                Set<String> firstQueryTableSet = calcTableSet(paramRelationList);
                String firstFromSql = QuerySqlUtil.toFromSql(tcInfo, mainTable, paramRelationList);
                String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !firstQueryTableSet.isEmpty(), param, params);
                return queryPage(firstFromSql, allFromSql, whereSql, mainTable, param, result, firstQueryTableSet, allTableSet, params);
            } else {
                String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !allTableSet.isEmpty(), param, params);
                return queryList(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            }
        } else {
            String whereSql = QuerySqlUtil.toWhereSql(tcInfo, mainTable, !allTableSet.isEmpty(), param, params);
            if (req.getType() == ReqResultType.OBJ) {
                return queryObj(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            } else {
                return queryListNoLimit(allFromSql + whereSql, mainTable, param, result, allTableSet, params);
            }
        }
    }

    private Set<String> calcTableSet(List<TableJoinRelation> paramRelationList) {
        Set<String> firstQueryTableSet = new HashSet<>();
        for (TableJoinRelation joinRelation : paramRelationList) {
            firstQueryTableSet.add(joinRelation.getMasterTable().getName());
            firstQueryTableSet.add(joinRelation.getChildTable().getName());
        }
        return firstQueryTableSet;
    }

    private Map<String, Object> queryPage(String firstFromSql, String allFromSql, String whereSql, String mainTable,
                                          ReqParam param, ReqResult result, Set<String> firstQueryTableSet,
                                          Set<String> allTableSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        long count;
        List<Map<String, Object>> pageList;
        if (result.needGroup()) {
            // SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... ) tmp    (only where's table)
            String selectCountGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, firstQueryTableSet, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectCountGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                String fromAndWhereList = allFromSql + whereSql;
                // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... LIMIT ...    (all where's table)
                String selectListGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhereList, mainTable, result, allTableSet, params);
                pageList = queryPageListWithGroup(selectListGroupSql, mainTable, !allTableSet.isEmpty(), param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            boolean needAlias = !firstQueryTableSet.isEmpty();
            // SELECT COUNT(DISTINCT id) FROM ... WHERE .?..   (only where's table)
            String countSql = QuerySqlUtil.toCountWithoutGroupSql(tcInfo, mainTable, needAlias, param, fromAndWhere);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(firstFromSql, allFromSql, whereSql, mainTable, param,
                        result, firstQueryTableSet, allTableSet, params);
            } else {
                pageList = Collections.emptyList();
            }
        }
        Map<String, Object> pageInfo = new LinkedHashMap<>();
        pageInfo.put("count", count);
        pageInfo.put("list", pageList);
        return pageInfo;
    }

    private long queryCount(String countSql, List<Object> params) {
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private List<Map<String, Object>> queryPageListWithoutGroup(String firstFromSql, String allFromSql,
                                                                String whereSql, String mainTable, ReqParam param,
                                                                ReqResult result, Set<String> firstQueryTableSet,
                                                                Set<String> allTableSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...   (only where's table)
            String idPageSql = QuerySqlUtil.toIdPageSql(tcInfo, fromAndWhere, mainTable, !firstQueryTableSet.isEmpty(), param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM .?. WHERE id IN (...)    (all where's table)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(tcInfo, mainTable, allFromSql, result, idList, allTableSet, params);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageWithoutGroupSql(tcInfo, fromAndWhere, mainTable, param, result, allTableSet, params);
        }
        return assemblyResult(sql, params, mainTable, !allTableSet.isEmpty(), result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainTable, boolean needAlias,
                                                             ReqParam param, ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, needAlias, result);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainTable, ReqParam param,
                                                ReqResult result, Set<String> allTableSet, List<Object> params) {
        boolean needAlias = !allTableSet.isEmpty();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, needAlias, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainTable, ReqParam param,
                                                       ReqResult result, Set<String> allTableSet, List<Object> params) {
        boolean needAlias = !allTableSet.isEmpty();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, params, mainTable, needAlias, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainTable, ReqParam param, ReqResult result,
                                         Set<String> allTableSet, List<Object> params) {
        boolean needAlias = !allTableSet.isEmpty();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tcInfo, fromAndWhere, mainTable, result, allTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tcInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, params, mainTable, needAlias, result));
        return (obj == null) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, List<Object> params, String mainTable,
                                                     boolean needAlias, ReqResult result) {
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (!mapList.isEmpty()) {
            // todo
            Table table = tcInfo.findTable(mainTable);
            List<String> idKeyList = table.getIdKey();
            Set<String> relationColumnSet = result.innerColumn(mainTable, tcInfo, needAlias);
            Map<String, List<Map<String, Object>>> innerColumnMap = queryInnerData(table, result);
        }
        return mapList;
    }

    private Map<String, List<Map<String, Object>>> queryInnerData(Table mainTable, ReqResult result) {
        Map<String, List<Map<String, Object>>> innerMap = new HashMap<>();
        for (Object obj : result.getColumns()) {
            if (obj != null) {
                if (!(obj instanceof String) && !(obj instanceof List<?>)) {
                    Map<String, ReqResult> inner = JsonUtil.convertType(obj, QueryConst.RESULT_TYPE);
                    for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                        String innerName = entry.getKey();
                        innerMap.put(innerName + "-id", queryInnerData(entry.getValue()));
                    }
                }
            }
        }
        return innerMap;
    }
    private List<Map<String, Object>> queryInnerData(ReqResult result) {
        return null;
    }
}
