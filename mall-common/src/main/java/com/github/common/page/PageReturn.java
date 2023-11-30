package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings({"unchecked", "rawtypes"})
public class PageReturn<T> {

    private static final PageReturn EMPTY = new PageReturn<>(0, Collections.emptyList());

    @ApiReturn("SELECT COUNT(*) FROM ... 的结果")
    private long total;

    @ApiReturn("SELECT ... FROM ... LIMIT 0, 10 的结果")
    private List<T> list;

    public PageReturn() {}
    public PageReturn(long total) {
        this.total = total;
    }
    public PageReturn(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public static <T> PageReturn<T> empty() {
        return EMPTY;
    }
    public static <T> PageReturn<T> total(long total) {
        return new PageReturn<>(total, Collections.emptyList());
    }
    public static <T> PageReturn<T> page(long total, List<T> list) {
        return new PageReturn<>(total, list);
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convertJustTotal(PageReturn<S> pageInfo) {
        if (U.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal());
        }
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convert(PageReturn<S> pageInfo, Class<T> clazz) {
        if (U.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal(), JsonUtil.convertList(pageInfo.getList(), clazz));
        }
    }
}
