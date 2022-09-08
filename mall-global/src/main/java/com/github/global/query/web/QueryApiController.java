package com.github.global.query.web;

import com.github.common.json.JsonResult;
import com.github.global.query.config.QuerySchemaInfoConfig;
import com.github.global.query.model.QueryInfo;
import com.github.global.query.model.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schema-info")
@RequiredArgsConstructor
public class QueryApiController {

    private final QuerySchemaInfoConfig schemaInfoConfig;

    @GetMapping("/list")
    public JsonResult<List<QueryInfo>> query() {
        return JsonResult.success("schema-info list", schemaInfoConfig.queryInfo());
    }

    @PostMapping("/query")
    public JsonResult<Object> query(@RequestBody RequestInfo req) {
        return JsonResult.success("query schema-info", schemaInfoConfig.query(req));
    }
}
