package com.github.global.query.web;

import com.github.common.json.JsonResult;
import com.github.global.query.config.QueryTableInfoConfig;
import com.github.global.query.model.QueryInfo;
import com.github.global.query.model.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/table-column")
@RequiredArgsConstructor
public class QueryApiController {

    @Value("${query.online:false}")
    private boolean online;

    private final QueryTableInfoConfig tableInfoConfig;

    @GetMapping("/list")
    public JsonResult<List<QueryInfo>> query(String tables) {
        return JsonResult.success("table info list", online ? null : tableInfoConfig.queryInfo(tables));
    }

    @PostMapping("/query")
    public JsonResult<Object> query(@RequestBody RequestInfo req) {
        return JsonResult.success("query table info", tableInfoConfig.query(req));
    }
}
