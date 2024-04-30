package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiReturn;

/** 深分页时返回, 分页返回 prev 和 next, 做为深分页时上一页下一页的入参 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageSearchReturn<T> extends PageReturn<T> {

    @ApiReturn("请求上一页时用此值拼 page_token 的值")
    private String prevToken;

    @ApiReturn("请求下一页时用此值拼 page_token 的值")
    private String nextToken;


    public String getPrevToken() {
        return prevToken;
    }
    public void setPrevToken(String prevToken) {
        this.prevToken = prevToken;
    }

    public String getNextToken() {
        return nextToken;
    }
    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }
}
