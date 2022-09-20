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
    private final TableColumnInfo tableColumnInfo;

    public QueryTableInfoConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableColumnInfo = (scanPackages == null || scanPackages.isEmpty())
                ? initWithDb() : QueryUtil.scanTable(scanPackages);
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
        for (Table table : tableColumnInfo.allTable()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (TableColumn column : table.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                Integer length = (column.getStrLen() == 0) ? null : column.getStrLen();
                TableColumnRelation relation = tableColumnInfo.findRelationByChild(table.getName(), column.getName());
                String rel = (relation == null) ? null : (relation.getOneTable() + "." + relation.getOneColumn());
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type, length, rel));
            }
            queryList.add(new QueryInfo(table.getAlias(), table.getDesc(), columnList));
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        Set<String> paramTableSet = req.checkParam(tableColumnInfo);
        Set<String> resultFunctionTableSet = req.checkResult(tableColumnInfo);

        String mainTable = req.getTable();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        List<TableJoinRelation> paramJoinRelationList = param.joinRelationList(tableColumnInfo,
                paramTableSet, resultFunctionTableSet);
        Set<String> firstQueryTableSet = new HashSet<>();
        for (TableJoinRelation joinRelation : paramJoinRelationList) {
            firstQueryTableSet.add(joinRelation.getMasterTable().getName());
            firstQueryTableSet.add(joinRelation.getChildTable().getName());
        }
        boolean needAlias = !paramJoinRelationList.isEmpty();

        List<Object> params = new ArrayList<>();

        String fromSql = QuerySqlUtil.toFromSql(tableColumnInfo, mainTable, paramJoinRelationList);
        String whereSql = QuerySqlUtil.toWhereSql(tableColumnInfo, mainTable, needAlias, param, params);

        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                return queryCountPage(fromSql, whereSql, mainTable, needAlias, param, result, firstQueryTableSet, params);
            } else {
                // 「移动端-瀑布流」时不需要「SELECT COUNT(*)」
                return queryListLimit(fromSql + whereSql, mainTable, needAlias, param, result, firstQueryTableSet, params);
            }
        } else {
            if (req.getType() == ReqResultType.OBJ) {
                return queryObj(fromSql + whereSql, mainTable, needAlias, param, result, firstQueryTableSet, params);
            } else {
                return queryListNoLimit(fromSql + whereSql, mainTable, needAlias, param, result, firstQueryTableSet, params);
            }
        }
    }

    private Map<String, Object> queryCountPage(String fromSql, String whereSql, String mainTable,
                                               boolean needAlias, ReqParam param, ReqResult result,
                                               Set<String> firstQueryTableSet, List<Object> params) {
        String fromAndWhere = fromSql + whereSql;
        long count;
        List<Map<String, Object>> pageList;
        if (result.notNeedGroup()) {
            String countSql = QuerySqlUtil.toCountWithoutGroupSql(tableColumnInfo, mainTable, needAlias, param, fromAndWhere);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(fromSql, whereSql, mainTable, needAlias,
                        param, result, firstQueryTableSet, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tableColumnInfo, fromAndWhere, mainTable, needAlias,
                    result, firstQueryTableSet, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithGroup(selectGroupSql, param, mainTable, needAlias, result, params);
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

    private List<Map<String, Object>> queryPageListWithoutGroup(String fromSql, String whereSql, String mainTable,
                                                                boolean needAlias, ReqParam param, ReqResult result,
                                                                Set<String> firstQueryTableSet, List<Object> params) {
        String fromAndWhere = fromSql + whereSql;
        // 很深的查询(深分页)时, 先用「条件 + 排序 + 分页」只查 id, 再用 id 查具体的数据列
        String sql;
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
            String idPageSql = QuerySqlUtil.toIdPageSql(tableColumnInfo, fromAndWhere, mainTable, needAlias, param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM ... WHERE id IN (x, y, z)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(tableColumnInfo, mainTable, fromSql, needAlias,
                    result, idList, firstQueryTableSet, params);
        } else {
            sql = QuerySqlUtil.toPageWithoutGroupSql(tableColumnInfo, fromAndWhere, mainTable, needAlias,
                    param, result, firstQueryTableSet, params);
        }
        return assemblyResult(sql, params, mainTable, needAlias, param, result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, ReqParam param,
                                                             String mainTable, boolean needAlias,
                                                             ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, needAlias, param, result);
    }

    private List<Map<String, Object>> queryListLimit(String fromAndWhere, String mainTable, boolean needAlias,
                                                     ReqParam param, ReqResult result,
                                                     Set<String> firstQueryTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tableColumnInfo, fromAndWhere, mainTable,
                needAlias, result, firstQueryTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tableColumnInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainTable, needAlias, param, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainTable, boolean needAlias,
                                                       ReqParam param, ReqResult result,
                                                       Set<String> firstQueryTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tableColumnInfo, fromAndWhere, mainTable,
                needAlias, result, firstQueryTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tableColumnInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, params, mainTable, needAlias, param, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainTable, boolean needAlias, ReqParam param,
                                         ReqResult result, Set<String> firstQueryTableSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(tableColumnInfo, fromAndWhere, mainTable,
                needAlias, result, firstQueryTableSet, params);
        String orderSql = param.generateOrderSql(mainTable, needAlias, tableColumnInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, params, mainTable, needAlias, param, result));
        return (obj == null) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String Sql, List<Object> params, String mainTable,
                                                     boolean needAlias, ReqParam param, ReqResult result) {
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(Sql, params.toArray());
        if (!mapList.isEmpty()) {
            // todo
            List<String> idKeyList = tableColumnInfo.findTable(mainTable).getIdKey();
            Map<String, List<Map<String, Object>>> otherColumnMap = queryOtherData(mainTable, result);
            Map<String, List<Map<String, Object>>> innerColumnMap = queryInnerData(mainTable, result);
        }
        return mapList;
    }
    private Map<String, List<Map<String, Object>>> queryOtherData(String mainTable, ReqResult result) {
        String otherSelectSql = result.generateOtherSelectSql();
        if (otherSelectSql == null || otherSelectSql.isEmpty()) {
            return Collections.emptyMap();
        }
        String otherFromSql = result.generateOtherFromSql();
        if (otherFromSql == null || otherFromSql.isEmpty()) {
            return Collections.emptyMap();
        }

        String otherSql = otherSelectSql + otherFromSql;
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(otherSql);
        return null;
    }
    private Map<String, List<Map<String, Object>>> queryInnerData(String mainTable, ReqResult result) {
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
