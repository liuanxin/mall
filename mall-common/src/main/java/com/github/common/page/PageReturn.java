package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageReturn<T> implements Serializable {
    private static final long serialVersionUID = 0L;

    @ApiReturn("SELECT COUNT(*) FROM ... 的结果")
    private long total;

    @ApiReturn("SELECT ... FROM ... LIMIT 0, 10 的结果")
    private List<T> list;

    public static <T> PageReturn<T> emptyReturn() {
        return new PageReturn<>(0, Collections.emptyList());
    }
    public static <T> PageReturn<T> returnPage(long total, List<T> list) {
        return new PageReturn<>(total, list);
    }

    /** 在 Controller 中调用 --> 组装不同的 vo 时使用此方法 */
    public static <S,T> PageReturn<T> convert(PageReturn<S> pageInfo) {
        if (U.isBlank(pageInfo)) {
            return emptyReturn();
        } else {
            // 只要总条数
            PageReturn<T> info = new PageReturn<>();
            info.setTotal(pageInfo.getTotal());
            return info;
        }
    }
}
