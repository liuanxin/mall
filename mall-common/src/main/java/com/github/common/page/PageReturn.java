package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings({"unchecked", "rawtypes"})
public class PageReturn<T> {

    private static final PageReturn EMPTY = new PageReturn<>(0, Collections.emptyList());

    @ApiReturn("SELECT COUNT(*) FROM ... 的结果")
    private long total;

    @ApiReturn("SELECT ... FROM ... LIMIT 0, 10 的结果")
    private List<T> list;

    public PageReturn(long total) {
        this.total = total;
    }

    public static <T> PageReturn<T> emptyReturn() {
        return EMPTY;
    }
    public static <T> PageReturn<T> returnTotal(long total) {
        return new PageReturn<>(total, Collections.emptyList());
    }
    public static <T> PageReturn<T> returnPage(long total, List<T> list) {
        return new PageReturn<>(total, list);
    }

    public static <T> PageReturn<T> page(long count, int index, int limit, Supplier<List<T>> supplier) {
        if (count == 0) {
            return EMPTY;
        }
        // 比如总条数有 100 条, index 是 11, limit 是 10, 这时候是没必要发起 limit 查询的, 只有 index 在 1 ~ 10 才需要
        if ((index == 1) || ((long) index * limit <= count)) {
            return new PageReturn<>(count, supplier.get());
        } else {
            return new PageReturn<>(count, Collections.emptyList());
        }
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
