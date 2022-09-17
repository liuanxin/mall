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
        // table_name, column_name, column_type, column_comment, has_pri, varchar_length
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
                Class<?> clazz = QueryUtil.mappingClass(QueryUtil.toStr(columnInfo.get("ct")));
                String columnName = QueryUtil.toStr(columnInfo.get("cn"));
                String columnAlias = QueryUtil.columnNameToAlias(columnName);
                String columnDesc = QueryUtil.toStr(columnInfo.get("cc"));
                boolean primary = "PRI".equalsIgnoreCase(QueryUtil.toStr(columnInfo.get("ck")));
                int strLen = QueryUtil.toInt(QueryUtil.toStr(columnInfo.get("cml")));

                aliasMap.put(QueryConst.COLUMN_PREFIX + columnName, columnAlias);
                columnMap.put(columnAlias, new SchemaColumn(columnName, columnDesc, columnAlias, primary, strLen, clazz));
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


    public List<QueryInfo> queryInfo() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : schemaColumnInfo.allSchema()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (SchemaColumn column : schema.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                Integer length = (column.getStrLen() == 0) ? null : column.getStrLen();
                SchemaColumnRelation relation = schemaColumnInfo.findRelationByChild(schema.getName(), column.getName());
                String rel = (relation == null) ? null : (relation.getOneSchema() + "." + relation.getOneColumn());
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type, length, rel));
            }
            queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        Set<String> paramSchema = req.check(schemaColumnInfo);

        String mainSchema = req.getSchema();
        ReqParam param = req.getParam();

        ReqResult result = req.getResult();

        List<Object> params = new ArrayList<>();
        String fromAndWhere = QuerySqlUtil.toFromWhereSql(schemaColumnInfo, mainSchema, paramSchema, param, params);

        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                return queryCountPage(fromAndWhere, mainSchema, paramSchema, param, result, params);
            } else {
                // 「移动端-瀑布流」时不需要「SELECT COUNT(*)」
                return queryListLimit(fromAndWhere, mainSchema, paramSchema, param, result, params);
            }
        } else {
            if (req.getType() == ReqResultType.OBJ) {
                return queryObj(fromAndWhere, mainSchema, paramSchema, param, result, params);
            } else {
                return queryListNoLimit(fromAndWhere, mainSchema, paramSchema, param, result, params);
            }
        }
    }

    private Map<String, Object> queryCountPage(String fromAndWhere, String mainSchema, Set<String> paramSchema,
                                               ReqParam param, ReqResult result, List<Object> params) {
        long count;
        List<Map<String, Object>> pageList;
        if (result.notNeedGroup()) {
            count = queryCount(QuerySqlUtil.toCountWithoutGroupSql(schemaColumnInfo, mainSchema, paramSchema, fromAndWhere), params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(fromAndWhere, mainSchema, paramSchema, param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            String selectGroupSql = QuerySqlUtil.toSelectGroupSql(schemaColumnInfo, fromAndWhere, mainSchema,
                    paramSchema, result, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithGroup(selectGroupSql, param, result, params);
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

    private List<Map<String, Object>> queryPageListWithoutGroup(String fromAndWhere, String mainSchema,
                                                                Set<String> paramSchema, ReqParam param,
                                                                ReqResult result, List<Object> params) {
        // 很深的查询(深分页)时, 先用「条件 + 排序 + 分页」只查 id, 再用 id 查具体的数据列
        String sql;
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE ... ORDER BY ... LIMIT ...
            String idPageSql = QuerySqlUtil.toIdPageSql(schemaColumnInfo, fromAndWhere, mainSchema, !paramSchema.isEmpty(), param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM ... WHERE id IN (x, y, z)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(schemaColumnInfo, mainSchema, paramSchema, result, idList, params);
        } else {
            sql = QuerySqlUtil.toPageWithoutGroupSql(schemaColumnInfo, fromAndWhere, mainSchema,
                    paramSchema, param, result, params);
        }
        return assemblyResult(sql, param, result, params);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, ReqParam param,
                                                             ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, param, result, params);
    }

    private List<Map<String, Object>> queryListLimit(String fromAndWhere, String mainSchema, Set<String> paramSchema,
                                                     ReqParam param, ReqResult result, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(schemaColumnInfo, fromAndWhere, mainSchema,
                paramSchema, result, params);
        String orderSql = param.generateOrderSql(mainSchema, !paramSchema.isEmpty(), schemaColumnInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, param, result, params);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainSchema, Set<String> paramSchema,
                                                       ReqParam param, ReqResult result, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(schemaColumnInfo, fromAndWhere, mainSchema,
                paramSchema, result, params);
        String orderSql = param.generateOrderSql(mainSchema, !paramSchema.isEmpty(), schemaColumnInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, param, result, params);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainSchema, Set<String> paramSchema,
                                         ReqParam param, ReqResult result, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(schemaColumnInfo, fromAndWhere, mainSchema,
                paramSchema, result, params);
        String orderSql = param.generateOrderSql(mainSchema, !paramSchema.isEmpty(), schemaColumnInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        List<Map<String, Object>> list = assemblyResult(sql, param, result, params);
        return QueryUtil.defaultIfNull(QueryUtil.first(list), Collections.emptyMap());
    }

    private List<Map<String, Object>> assemblyResult(String Sql, ReqParam param, ReqResult result, List<Object> params) {
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(Sql, params.toArray());

        List<Map<String, Object>> returnList = new ArrayList<>();
        if (!mapList.isEmpty()) {
            for (Map<String, Object> data : mapList) {
                // todo
            }
        }
        return returnList;
    }
}
