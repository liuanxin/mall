package com.github.global.query.web;

import com.github.common.json.JsonResult;
import com.github.global.query.config.QuerySchemeInfo;
import com.github.global.query.model.QueryInfo;
import com.github.global.query.model.RequestInfo;
import com.github.global.query.model.Scheme;
import com.github.global.query.model.SchemeColumn;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/scheme-info")
@RequiredArgsConstructor
public class QueryApiController {

    private final QuerySchemeInfo schemeInfo;

    @GetMapping
    public JsonResult<List<QueryInfo>> query() {
        List<QueryInfo> queryList = new ArrayList<>();
        for (Scheme scheme : schemeInfo.getTableColumnInfo().getSchemeMap().values()) {
            List<QueryInfo.QueryColumn> columnList = new ArrayList<>();
            for (SchemeColumn column : scheme.getColumnMap().values()) {
                String type = column.getColumnType().getSimpleName();
                columnList.add(new QueryInfo.QueryColumn(column.getAlias(), column.getDesc(), type));
            }
            queryList.add(new QueryInfo(scheme.getAlias(), scheme.getDesc(), columnList));
        }
        return JsonResult.success("scheme info", queryList);
    }

    @PostMapping
    public JsonResult<Object> query(@RequestBody RequestInfo req) {
        return JsonResult.success("query scheme-info");
    }
}
