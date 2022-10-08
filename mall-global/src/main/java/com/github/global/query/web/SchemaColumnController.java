package com.github.global.query.web;

import com.github.common.json.JsonResult;
import com.github.global.query.config.QuerySchemaInfoHandler;
import com.github.global.query.model.QueryInfo;
import com.github.global.query.model.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schema-column")
@RequiredArgsConstructor
public class SchemaColumnController {

    @Value("${query.online:false}")
    private boolean online;

    private final QuerySchemaInfoHandler schemaInfoConfig;

    @GetMapping
    public JsonResult<List<QueryInfo>> info(String schemas) {
        return JsonResult.success("schema info list", online ? null : schemaInfoConfig.info(schemas));
    }

    @PostMapping
    public JsonResult<Object> query(@RequestBody RequestInfo req) {
        return JsonResult.success("query schema info", schemaInfoConfig.query(req));
    }
}
