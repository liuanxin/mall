package com.github.config;

import com.github.common.Const;
import com.github.common.json.JsonCode;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.EnableApiInfo;
import com.github.liuanxin.api.model.DocumentCopyright;
import com.github.liuanxin.api.model.DocumentParam;
import com.github.liuanxin.api.model.DocumentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@EnableApiInfo
@ConditionalOnClass(DocumentCopyright.class)
public class BackendApiInfoConfig {

    @Value("${online:false}")
    private boolean online;

    @Bean
    public DocumentCopyright urlCopyright() {
        return new DocumentCopyright()
                .setTitle("后端服务-" + Develop.TITLE)
                .setTeam(Develop.TEAM)
                .setCopyright(Develop.COPYRIGHT)
                .setVersion(Develop.VERSION)
                .setOnline(online)
                .setIgnoreUrlSet(ignoreUrl())
                .setGlobalResponse(globalResponse())
                .setGlobalTokens(tokens());
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

    private List<DocumentParam> tokens() {
        return Collections.singletonList(
                DocumentParam.buildToken(Const.TOKEN, "用户认证数据", "", true)
                // DocumentParam.buildToken(Const.VERSION, "接口版本", "1.0", false)
        );
    }
}
