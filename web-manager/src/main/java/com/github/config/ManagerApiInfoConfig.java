package com.github.config;

import com.github.common.json.JsonCode;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.EnableApiInfo;
import com.github.liuanxin.api.model.DocumentCopyright;
import com.github.liuanxin.api.model.DocumentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@EnableApiInfo
@ConditionalOnClass(DocumentCopyright.class)
public class ManagerApiInfoConfig {

    @Value("${online:false}")
    private boolean online;

    @Bean
    public DocumentCopyright urlCopyright() {
        return new DocumentCopyright()
                .setTitle("后台管理-" + Develop.TITLE)
                .setTeam(Develop.TEAM)
                .setCopyright(Develop.COPYRIGHT)
                .setVersion(Develop.VERSION)
                .setOnline(online)
                .setIgnoreUrlSet(ignoreUrl())
                .setGlobalResponse(globalResponse());
    }

    private Set<String> ignoreUrl() {
        return new HashSet<>(Collections.singleton("/error"));
    }

    private List<DocumentResponse> globalResponse() {
        List<DocumentResponse> responseList = new ArrayList<>();
        for (JsonCode code : JsonCode.values()) {
            responseList.add(new DocumentResponse(code.getCode(), code.getValue()));
        }
        return responseList;
    }
}
