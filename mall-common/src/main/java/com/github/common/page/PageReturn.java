package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.common.util.Obj;
import com.github.liuanxin.api.annotation.ApiReturn;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings({"unchecked", "rawtypes"})
public class PageReturn<T> {

    private static final PageReturn EMPTY = new PageReturn<>(0, Collections.emptyList(), null);

    @ApiReturn("总条数: SELECT COUNT(*) FROM ... 的结果")
    private long total;

    @ApiReturn("当页数据: SELECT ... FROM ... LIMIT 0, 10 的结果")
    private List<T> list;

    @ApiReturn("总计项")
    private T summary;

    public PageReturn() {}
    public PageReturn(long total, List<T> list, T summary) {
        this.total = total;
        this.list = list;
        this.summary = summary;
    }

    public long getTotal() {
        return total;
    }
    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }
    public void setList(List<T> list) {
        this.list = list;
    }

    public T getSummary() {
        return summary;
    }
    public void setSummary(T summary) {
        this.summary = summary;
    }


    public static <T> PageReturn<T> empty() {
        return EMPTY;
    }
    public static <T> PageReturn<T> total(long total) {
        return new PageReturn<>(total, Collections.emptyList(), null);
    }
    public static <T> PageReturn<T> page(long total, List<T> list) {
        return new PageReturn<>(total, list, null);
    }
    public static <T> PageReturn<T> pageAndSummary(long total, List<T> list, T summary) {
        return new PageReturn<>(total, list, summary);
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convertTotal(PageReturn<S> pageInfo) {
        if (Obj.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal(), Collections.emptyList(), null);
        }
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convert(PageReturn<S> pageInfo, Class<T> clazz) {
        if (Obj.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal(), JsonUtil.convertList(pageInfo.getList(), clazz), null);
        }
    }
}
