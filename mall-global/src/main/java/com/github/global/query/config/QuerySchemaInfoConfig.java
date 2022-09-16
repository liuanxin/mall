package com.github.global.query.config;

import com.github.common.collection.MapMultiUtil;
import com.github.common.collection.MapMultiValue;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ReqResultType;
import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.*;
import com.github.global.query.util.QuerySqlUtil;
import com.github.global.query.util.QueryUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QuerySchemaInfoConfig {

    @Value("${query.scan-packages:}")
    private String scanPackages;

    @Value("${query.deep-max-page-size:10000}")
    private int deepMaxPageSize;

    private final JdbcTemplate jdbcTemplate;
    private final SchemaColumnInfo schemaColumnInfo;

    public QuerySchemaInfoConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaColumnInfo = (scanPackages == null || scanPackages.isEmpty())
                ? initWithDb() : QueryUtil.scanSchema(scanPackages);
    }
    private SchemaColumnInfo initWithDb() {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        List<SchemaColumnRelation> relationList = new ArrayList<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // table_name, table_comment
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        // table_name, column_name, column_type, column_comment, has_pri
        List<Map<String, Object>> schemaColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        // table_name, column_name, relation_table_name, relation_column_name (relation : one or many)
        List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
        // table_name, column_name, has_single_unique
        List<Map<String, Object>> indexList = jdbcTemplate.queryForList(QueryConst.INDEX_SQL, dbName);

        MapMultiValue<String, Map<String, Object>, List<Map<String, Object>>> schemaColumnMap = MapMultiUtil.createMapList();
        if (!schemaColumnList.isEmpty()) {
            for (Map<String, Object> schemaColumn : schemaColumnList) {
                schemaColumnMap.put(QueryUtil.toStr(schemaColumn.get("tn")), schemaColumn);
            }
        }
        Map<String, Map<String, Map<String, Object>>> relationColumnMap = new HashMap<>();
        if (!relationColumnList.isEmpty()) {
            for (Map<String, Object> relationColumn : relationColumnList) {
                String schemaName = QueryUtil.toStr(relationColumn.get("tn"));
                Map<String, Map<String, Object>> columnMap = relationColumnMap.getOrDefault(schemaName, new HashMap<>());
                columnMap.put(QueryUtil.toStr(relationColumn.get("cn")), relationColumn);
                relationColumnMap.put(schemaName, columnMap);
            }
        }
        Map<String, Set<String>> columnUniqueMap = new HashMap<>();
        if (!indexList.isEmpty()) {
            for (Map<String, Object> index : indexList) {
                String schemaName = QueryUtil.toStr(index.get("tn"));
                Set<String> uniqueColumnSet = columnUniqueMap.getOrDefault(schemaName, new HashSet<>());
                uniqueColumnSet.add(QueryUtil.toStr(index.get("cn")));
                columnUniqueMap.put(schemaName, uniqueColumnSet);
            }
        }

        for (Map<String, Object> schemaInfo : schemaList) {
            String schemaName = QueryUtil.toStr(schemaInfo.get("tn"));
            String schemaAlias = QueryUtil.schemaNameToAlias(schemaName);
            String schemaDesc = QueryUtil.toStr(schemaInfo.get("tc"));
            Map<String, SchemaColumn> columnMap = new LinkedHashMap<>();

            List<Map<String, Object>> columnList = schemaColumnMap.get(schemaName);
            for (Map<String, Object> columnInfo : columnList) {
                Class<?> clazz = mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToAlias(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new SchemaColumn(columnName, columnDesc, columnAlias, primary, clazz));
            }
            aliasMap.put(QueryConst.SCHEMA_PREFIX + schemaName, schemaAlias);
            schemaMap.put(schemaAlias, new Schema(schemaName, schemaDesc, schemaAlias, columnMap));
        }

        if (!relationColumnMap.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<String, Object>>> entry : relationColumnMap.entrySet()) {
                String relationSchema = entry.getKey();
                Set<String> uniqueColumnSet = columnUniqueMap.get(relationSchema);
                for (Map.Entry<String, Map<String, Object>> columnEntry : entry.getValue().entrySet()) {
                    String relationColumn = columnEntry.getKey();
                    SchemaRelationType type = uniqueColumnSet.contains(relationColumn)
                            ? SchemaRelationType.ONE_TO_ONE : SchemaRelationType.ONE_TO_MANY;

                    Map<String, Object> relationInfoMap = columnEntry.getValue();
                    String oneSchema = QueryUtil.toStr(relationInfoMap.get("ftn"));
                    String oneColumn = QueryUtil.toStr(relationInfoMap.get("fcn"));

                    relationList.add(new SchemaColumnRelation(oneSchema, oneColumn, type, relationSchema, relationColumn));
                }
            }
        }
        return new SchemaColumnInfo(aliasMap, schemaMap, relationList);
    }
    private Class<?> mappingClass(String dbType) {
        String type = (dbType.contains("(") ? dbType.substring(0, dbType.indexOf("(")) : dbType).toLowerCase();
        for (Map.Entry<String, Class<?>> entry : QueryConst.DB_TYPE_MAP.entrySet()) {
            if (type.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        throw new RuntimeException("unknown db type" + dbType);
    }


    public List<QueryInfo> queryInfo() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : schemaColumnInfo.allSchema()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (SchemaColumn column : schema.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                String schemaAndColumn = schema.getName() + "." + column.getName();
                SchemaColumnRelation columnRelation = schemaColumnInfo.findRelationByChild(schemaAndColumn);
                String relation = (columnRelation != null) ?
                        (columnRelation.getOneSchema() + "." + columnRelation.getOneColumn()) : null;
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type, relation));
            }
            queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        req.check(schemaColumnInfo);

        String mainSchema = req.getSchema();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        List<Object> params = new ArrayList<>();
        String fromAndWhere = QuerySqlUtil.toFromWhereSql(schemaColumnInfo, mainSchema, param, params);

        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                return pageReturn(fromAndWhere, mainSchema, param, result, params);
            } else {
                // 「移动端-瀑布流」时不需要「SELECT COUNT(*)」
                return pageList(fromAndWhere, mainSchema, param, result, params);
            }
        } else {
            if (result.getType() == ReqResultType.OBJ) {
                return queryObj(fromAndWhere, mainSchema, param, result, params);
            } else {
                return queryList(fromAndWhere, mainSchema, param, result, params);
            }
        }
    }

    private Map<String, Object> pageReturn(String fromAndWhere, String mainSchema, ReqParam param,
                                           ReqResult result, List<Object> params) {
        long count;
        List<Map<String, Object>> pageList;
        if (result.generateGroupSql().isEmpty()) {
            count = queryCount(QuerySqlUtil.toCountSql(schemaColumnInfo, mainSchema, param, fromAndWhere), params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageList(fromAndWhere, mainSchema, param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            String selectSql = QuerySqlUtil.toSelectSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectSql), params);
            if (param.needQueryCurrentPage(count)) {
                pageList = querySqlList(QuerySqlUtil.toPageGroupSql(selectSql, param, params), param, result, params);
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
        return QueryUtil.toLong(jdbcTemplate.queryForObject(countSql, Long.class, params.toArray()), 0);
    }

    private List<Map<String, Object>> queryPageList(String fromAndWhere, String mainSchema, ReqParam param,
                                                    ReqResult result, List<Object> params) {
        List<Map<String, Object>> list;
        // 很深的查询(深分页)时, 先用「条件 + 排序 + 分页」只查 id, 再用 id 查具体的数据列
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
            String idPageSql = QuerySqlUtil.toIdPageSql(schemaColumnInfo, fromAndWhere, mainSchema, param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM ... WHERE id IN (x, y, z)
            List<Object> idFromParams = new ArrayList<>();
            String idFromSql = QuerySqlUtil.toSelectSqlWithId(schemaColumnInfo, mainSchema, param, result, idList, idFromParams);
            list = jdbcTemplate.queryForList(idFromSql, idFromParams.toArray());
        } else {
            String pageSql = QuerySqlUtil.toPageSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
            list = jdbcTemplate.queryForList(pageSql, params.toArray());
        }
        assemblyResult(list, param, result);
        return list;
    }

    private List<Map<String, Object>> querySqlList(String listSql, ReqParam param, ReqResult result, List<Object> params) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(listSql, params.toArray());
        assemblyResult(list, param, result);
        return list;
    }

    private List<Map<String, Object>> pageList(String fromAndWhere, String mainSchema, ReqParam param,
                                               ReqResult result, List<Object> params) {
        String pageSql = QuerySqlUtil.toPageSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(pageSql, params.toArray());
        assemblyResult(list, param, result);
        return list;
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainSchema, ReqParam param,
                                                ReqResult result, List<Object> params) {
        String listSql = QuerySqlUtil.toListSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(listSql, params.toArray());
        assemblyResult(list, param, result);
        return list;
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainSchema, ReqParam param,
                                         ReqResult result, List<Object> params) {
        String objSql = QuerySqlUtil.toObjSql(schemaColumnInfo, fromAndWhere, mainSchema, param, result, params);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(objSql, params.toArray());
        assemblyResult(list, param, result);
        return list.isEmpty() ? Collections.emptyMap() : list.get(0);
    }

    private void assemblyResult(List<Map<String, Object>> list, ReqParam param, ReqResult result) {
        if (list != null && !list.isEmpty()) {
            // todo
            for (Map<String, Object> data : list) {
            }
        }
    }
}
