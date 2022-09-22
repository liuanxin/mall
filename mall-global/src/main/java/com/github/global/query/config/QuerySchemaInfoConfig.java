package com.github.global.query.config;

import com.github.common.collection.MapMultiUtil;
import com.github.common.collection.MapMultiValue;
import com.github.global.query.constant.QueryConst;
import com.github.global.query.enums.ResultType;
import com.github.global.query.enums.SchemaRelationType;
import com.github.global.query.model.*;
import com.github.global.query.util.QueryJsonUtil;
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
    private final SchemaColumnInfo scInfo;

    public QuerySchemaInfoConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.scInfo = (scanPackages == null || scanPackages.isEmpty()) ? initWithDb() : QueryUtil.scanSchema(scanPackages);
    }
    private SchemaColumnInfo initWithDb() {
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Schema> schemaMap = new LinkedHashMap<>();
        List<SchemaColumnRelation> relationList = new ArrayList<>();

        String dbName = jdbcTemplate.queryForObject(QueryConst.DB_SQL, String.class);
        // schema_name, schema_comment
        List<Map<String, Object>> schemaList = jdbcTemplate.queryForList(QueryConst.SCHEMA_SQL, dbName);
        // schema_name, column_name, column_type, column_comment, has_pri, varchar_length
        List<Map<String, Object>> schemaColumnList = jdbcTemplate.queryForList(QueryConst.COLUMN_SQL, dbName);
        // schema_name, column_name, relation_schema_name, relation_column_name (relation : one or many)
        List<Map<String, Object>> relationColumnList = jdbcTemplate.queryForList(QueryConst.RELATION_SQL, dbName);
        // schema_name, column_name, has_single_unique
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


    public List<QueryInfo> queryInfo(String schemas) {
        Set<String> schemaSet = new LinkedHashSet<>();
        if (schemas != null && !schemas.isEmpty()) {
            for (String te : schemas.split(",")) {
                String trim = te.trim();
                if (!trim.isEmpty()) {
                    schemaSet.add(trim.toLowerCase());
                }
            }
        }
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : scInfo.allSchema()) {
            if (schemaSet.isEmpty() || schemaSet.contains(schema.getAlias().toLowerCase())) {
                List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
                for (SchemaColumn column : schema.getColumnMap().values()) {
                    String type = column.getColumnType().getSimpleName();
                    Integer length = (column.getStrLen() == 0) ? null : column.getStrLen();
                    SchemaColumnRelation relation = scInfo.findRelationByChild(schema.getName(), column.getName());
                    String schemaColumn = (relation == null) ? null : (relation.getOneSchema() + "." + relation.getOneColumn());
                    columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type, length, schemaColumn));
                }
                queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
            }
        }
        return queryList;
    }

    public Object query(RequestInfo req) {
        req.checkSchema(scInfo);
        Set<String> paramSchemaSet = req.checkParam(scInfo);
        Set<String> resultFuncSchemaSet = req.checkResult(scInfo);

        String mainSchema = req.getSchema();
        ReqParam param = req.getParam();
        ReqResult result = req.getResult();

        List<SchemaJoinRelation> allRelationList = param.allRelationList(scInfo, mainSchema);
        Set<String> allSchemaSet = calcSchemaSet(allRelationList);
        String allFromSql = QuerySqlUtil.toFromSql(scInfo, mainSchema, allRelationList);

        List<Object> params = new ArrayList<>();
        if (param.needQueryPage()) {
            if (param.needQueryCount()) {
                List<SchemaJoinRelation> paramRelationList = param.paramRelationList(scInfo, mainSchema, paramSchemaSet, resultFuncSchemaSet);
                Set<String> firstQuerySchemaSet = calcSchemaSet(paramRelationList);
                String firstFromSql = QuerySqlUtil.toFromSql(scInfo, mainSchema, paramRelationList);
                String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !firstQuerySchemaSet.isEmpty(), param, params);
                return queryPage(firstFromSql, allFromSql, whereSql, mainSchema, param, result, firstQuerySchemaSet, allSchemaSet, params);
            } else {
                String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !allSchemaSet.isEmpty(), param, params);
                return queryList(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            }
        } else {
            String whereSql = QuerySqlUtil.toWhereSql(scInfo, mainSchema, !allSchemaSet.isEmpty(), param, params);
            if (req.getType() == ResultType.OBJ) {
                return queryObj(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            } else {
                return queryListNoLimit(allFromSql + whereSql, mainSchema, param, result, allSchemaSet, params);
            }
        }
    }

    private Set<String> calcSchemaSet(List<SchemaJoinRelation> paramRelationList) {
        Set<String> firstQuerySchemaSet = new HashSet<>();
        for (SchemaJoinRelation joinRelation : paramRelationList) {
            firstQuerySchemaSet.add(joinRelation.getMasterSchema().getName());
            firstQuerySchemaSet.add(joinRelation.getChildSchema().getName());
        }
        return firstQuerySchemaSet;
    }

    private Map<String, Object> queryPage(String firstFromSql, String allFromSql, String whereSql, String mainSchema,
                                          ReqParam param, ReqResult result, Set<String> firstQuerySchemaSet,
                                          Set<String> allSchemaSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        long count;
        List<Map<String, Object>> pageList;
        if (result.needGroup()) {
            // SELECT COUNT(*) FROM ( SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... ) tmp    (only where's schema)
            String selectCountGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, firstQuerySchemaSet, params);
            count = queryCount(QuerySqlUtil.toCountGroupSql(selectCountGroupSql), params);
            if (param.needQueryCurrentPage(count)) {
                String fromAndWhereList = allFromSql + whereSql;
                // SELECT ... FROM ... WHERE .?. GROUP BY ... HAVING ... LIMIT ...    (all where's schema)
                String selectListGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhereList, mainSchema, result, allSchemaSet, params);
                pageList = queryPageListWithGroup(selectListGroupSql, mainSchema, allSchemaSet, param, result, params);
            } else {
                pageList = Collections.emptyList();
            }
        } else {
            boolean needAlias = !firstQuerySchemaSet.isEmpty();
            // SELECT COUNT(DISTINCT id) FROM ... WHERE .?..   (only where's schema)
            String countSql = QuerySqlUtil.toCountWithoutGroupSql(scInfo, mainSchema, needAlias, param, fromAndWhere);
            count = queryCount(countSql, params);
            if (param.needQueryCurrentPage(count)) {
                pageList = queryPageListWithoutGroup(firstFromSql, allFromSql, whereSql, mainSchema, param,
                        result, firstQuerySchemaSet, allSchemaSet, params);
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
                                                                String whereSql, String mainSchema, ReqParam param,
                                                                ReqResult result, Set<String> firstQuerySchemaSet,
                                                                Set<String> allSchemaSet, List<Object> params) {
        String fromAndWhere = firstFromSql + whereSql;
        String sql;
        // deep paging(need offset a lot of result), use 「where + order + limit」 to query id, then use id to query specific columns
        if (param.hasDeepPage(deepMaxPageSize)) {
            // SELECT id FROM ... WHERE .?. ORDER BY ... LIMIT ...   (only where's schema)
            String idPageSql = QuerySqlUtil.toIdPageSql(scInfo, fromAndWhere, mainSchema, !firstQuerySchemaSet.isEmpty(), param, params);
            List<Map<String, Object>> idList = jdbcTemplate.queryForList(idPageSql, params.toArray());

            // SELECT ... FROM .?. WHERE id IN (...)    (all where's schema)
            params.clear();
            sql = QuerySqlUtil.toSelectWithIdSql(scInfo, mainSchema, allFromSql, result, idList, allSchemaSet, params);
        } else {
            // SELECT ... FROM ... WHERE ... ORDER BY ... limit ...
            sql = QuerySqlUtil.toPageWithoutGroupSql(scInfo, fromAndWhere, mainSchema, param, result, allSchemaSet, params);
        }
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryPageListWithGroup(String selectGroupSql, String mainSchema, Set<String> allSchemaSet,
                                                             ReqParam param, ReqResult result, List<Object> params) {
        String sql = selectGroupSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryList(String fromAndWhere, String mainSchema, ReqParam param,
                                                ReqResult result, Set<String> allSchemaSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        String sql = selectGroupSql + orderSql + param.generatePageSql(params);
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private List<Map<String, Object>> queryListNoLimit(String fromAndWhere, String mainSchema, ReqParam param,
                                                       ReqResult result, Set<String> allSchemaSet, List<Object> params) {
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, !allSchemaSet.isEmpty(), scInfo);
        String sql = selectGroupSql + orderSql;
        return assemblyResult(sql, params, mainSchema, allSchemaSet, result);
    }

    private Map<String, Object> queryObj(String fromAndWhere, String mainSchema, ReqParam param, ReqResult result,
                                         Set<String> allSchemaSet, List<Object> params) {
        boolean needAlias = !allSchemaSet.isEmpty();
        String selectGroupSql = QuerySqlUtil.toSelectGroupSql(scInfo, fromAndWhere, mainSchema, result, allSchemaSet, params);
        String orderSql = param.generateOrderSql(mainSchema, needAlias, scInfo);
        String sql = selectGroupSql + orderSql + param.generateArrToObjSql(params);
        Map<String, Object> obj = QueryUtil.first(assemblyResult(sql, params, mainSchema, allSchemaSet, result));
        return (obj == null) ? Collections.emptyMap() : obj;
    }

    private List<Map<String, Object>> assemblyResult(String mainSql, List<Object> params, String mainSchema,
                                                     Set<String> allSchemaSet, ReqResult result) {
        List<Map<String, Object>> mapList = jdbcTemplate.queryForList(mainSql, params.toArray());
        if (!mapList.isEmpty()) {
            boolean needAlias = !allSchemaSet.isEmpty();
            Schema schema = scInfo.findSchema(mainSchema);
            List<String> idKeyList = schema.getIdKey();

            Set<String> selectColumnSet = result.selectColumn(mainSchema, scInfo, allSchemaSet);
            List<String> needRemoveColumnList = new ArrayList<>();
            for (String ic : result.innerColumn(mainSchema, scInfo, needAlias)) {
                if (!selectColumnSet.contains(ic)) {
                    needRemoveColumnList.add(ic);
                }
            }

            Map<String, List<Map<String, Object>>> innerColumnMap = queryInnerData(schema, result);
            for (Map<String, Object> data : mapList) {
                fillInnerData(data, idKeyList, innerColumnMap);
                needRemoveColumnList.forEach(data::remove);
                result.handleDateType(data, mainSchema, scInfo);
            }
        }
        return mapList;
    }


    private void fillInnerData(Map<String, Object> data, List<String> idKeyList,
                               Map<String, List<Map<String, Object>>> innerColumnMap) {
    }

    private Map<String, List<Map<String, Object>>> queryInnerData(Schema mainSchema, ReqResult result) {
        Map<String, List<Map<String, Object>>> innerMap = new HashMap<>();
        for (Object obj : result.getColumns()) {
            if (obj != null) {
                if (!(obj instanceof String) && !(obj instanceof List<?>)) {
                    Map<String, ReqResult> inner = QueryJsonUtil.convertResult(obj);
                    if (inner != null) {
                        for (Map.Entry<String, ReqResult> entry : inner.entrySet()) {
                            String innerName = entry.getKey();
                            innerMap.put(innerName + "-id", queryInnerData(entry.getValue()));
                        }
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
