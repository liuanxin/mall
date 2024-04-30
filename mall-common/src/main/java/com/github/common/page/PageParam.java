package com.github.common.page;

import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiParamIgnore;

public class PageParam {

    /** 分页默认页 */
    private static final int DEFAULT_PAGE_NO = 1;
    /** 分页默认的每页条数 */
    private static final int DEFAULT_LIMIT = 10;
    /** 最大分页条数 */
    private static final int MAX_LIMIT = 1000;

    @ApiParam("当前页数. 不传 或 传入负数 或 传入非数字 则默认是 " + DEFAULT_PAGE_NO)
    private Integer page;

    @ApiParam("每页条数. 不传 或 传入负数 或 传入非数字 或 传入大于 " + MAX_LIMIT + " 的数则默认是 " + DEFAULT_LIMIT)
    private Integer limit;
    /** 是否是移动端 */
    @ApiParamIgnore
    private Boolean wasMobile = false;


    public Integer getPage() {
        return (page == null || page <= 0) ? DEFAULT_PAGE_NO : page;
    }
    public void setPage(Integer page) {
        this.page = ((page == null || page <= 0) ? DEFAULT_PAGE_NO : page);
    }

    public Integer getLimit() {
        return (limit == null || limit <= 0 || limit > MAX_LIMIT) ? DEFAULT_LIMIT : limit;
    }
    public void setLimit(Integer limit) {
        this.limit = ((limit == null || limit <= 0 || limit > MAX_LIMIT) ? DEFAULT_LIMIT : limit);
    }

    public Boolean getWasMobile() {
        return wasMobile != null && wasMobile;
    }
    public void setWasMobile(Boolean wasMobile) {
        this.wasMobile = (wasMobile != null && wasMobile);
    }

    /** 分页语句  LIMIT x, xx  中  x  的值 */
    public int pageStart() {
        return (page - 1) * limit;
    }
}
