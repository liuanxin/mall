package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiParam;

/** 深分页时用到, 入参添加 pageToken 用来处理深分页 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageSearchParam extends PageParam {

    @ApiParam("深分页时用到, 比如查询第 10000 页时接口返回了 prev_token: 111 和 next_token: 222, 此时想要请求第 9999 页时使用 pageToken=111, 想要请求第 10001 页时使用 pageToken=222")
    private String pageToken;

    public String getPageToken() {
        return pageToken;
    }
    public void setPageToken(String pageToken) {
        this.pageToken = pageToken;
    }
}
