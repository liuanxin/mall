package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiParam;

/** 使用 es 时返回的实体, 入参添加 pageToken 用来处理深分页 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageSearchParam extends PageParam {

    @ApiParam("使用 es 查询时返回的页面值, 接口有返回就拼上此值(上页使用 prev_token, 下页使用 next_token)")
    private String pageToken;

    public String getPageToken() {
        return pageToken;
    }
    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }
}
