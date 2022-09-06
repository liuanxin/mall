package com.github.global.query.web;

import com.github.common.json.JsonResult;
import com.github.global.query.config.QuerySchemaInfo;
import com.github.global.query.model.QueryInfo;
import com.github.global.query.model.RequestInfo;
import com.github.global.query.model.Schema;
import com.github.global.query.model.SchemaColumn;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/schema-info")
@RequiredArgsConstructor
public class QueryApiController {

    private final QuerySchemaInfo schemaInfo;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public JsonResult<List<QueryInfo>> query() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Schema schema : schemaInfo.getTableColumnInfo().getSchemaMap().values()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (SchemaColumn column : schema.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type));
            }
            queryList.add(new QueryInfo(schema.getAlias(), schema.getDesc(), columnList));
        }
        return JsonResult.success("schema info", queryList);
    }

    @PostMapping("/query")
    public JsonResult<Object> query(@RequestBody RequestInfo req) {
        req.check(schemaInfo.getTableColumnInfo());
        return JsonResult.success("query schema-info");
    }
}
