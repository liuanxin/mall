package com.github.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.json.JsonResult;
import com.github.global.service.ValidatorService;
import com.github.liuanxin.api.annotation.ApiIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@ApiIgnore
@RestController
@RequiredArgsConstructor
public class BackendTestController {

    @Value("${test:true}")
    private boolean hasTest;

    private final ValidatorService validatorService;

    @PostMapping("/test1")
    public JsonResult<String> test(@Validated @RequestBody Test test) {
        return JsonResult.success("test1");
    }

    @PostMapping("/test2")
    public JsonResult<String> test2(@RequestBody Test test) {
        // {"abcName":"a","modelMap":{"abc":{"id":"b"},"xyz":{"types":null}},"modelList":[{"id":"c"},{"types":null}]}
        if (hasTest) {
            validatorService.handleValidate(test);
        }
        return JsonResult.success("test2");
    }

    @Data
    public static class Test {
        @JsonProperty("abcName")
        @Size(min = 2, max = 5, message = "{404}")
        private String name;

        @JsonProperty("showGender")
        @NotNull(message = "{400}")
        private Integer gender;

        @Valid
        @JsonProperty("modelMap")
        private Map<String, Test1> model;

        @Valid
        @JsonProperty("modelList")
        private List<Test1> models;
    }
    @Data
    public static class Test1 {
        @Size(min = 2, max = 5, message = "xxx")
        private String id;

        @NotNull(message = "{403}")
        @JsonProperty("types")
        private Integer type;
    }
}
