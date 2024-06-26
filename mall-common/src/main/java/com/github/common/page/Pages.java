package com.github.common.page;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import com.mybatisflex.core.paginate.Page;

import java.util.Collections;
import java.util.List;

/** 当 controller 层无需引用 mbp 包时只需要在 service 层调用当前工具类 */
public final class Pages {

    /** 在 service 的实现类中调用 --> 当不想查 select count(*) 时用这个 */
    public static <T> Page<T> paramOnlyLimit(int limit) {
        return new Page<>(1, Math.max(limit, 1), 1);
    }

    public static <T> List<T> returnList(Page<T> pageInfo) {
        return U.isNull(pageInfo) ? Collections.emptyList() : pageInfo.getRecords();
    }

    public static <T> T returnOne(Page<T> pageInfo) {
        return U.isNull(pageInfo) ? null : A.first(pageInfo.getRecords());
    }

    public static <T> boolean hasExists(Page<T> pageInfo) {
        return U.isNotNull(pageInfo) && U.isNotNull(A.first(pageInfo.getRecords()));
    }

    public static <T> boolean hasNotExists(Page<T> pageInfo) {
        return !hasExists(pageInfo);
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 mbp 的 Page 对象, service 上的参数是 PageParam, 使用此方法进行转换 */
    public static <T> Page<T> param(PageParam page) {
        // 移动端与 pc 端的分页不同, 前者的用户习惯是一直刷, 一边刷一边加载, 它是不需要查询 select count(*) 的
        // 移动端也不需要有当前页的概念, 如果数据是按时间倒序(时间越后越排在前), 从下往上刷时, 它只需要加载比最下面的时间小的数据即可
        return page.getWasMobile() ? paramOnlyLimit(page.getLimit()) : new Page<>(page.getPage(), page.getLimit());
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的返回类型是 mbp 的 Page 对象, service 上的返回类型是 PageReturn, 使用此方法进行转换 */
    public static <T> PageReturn<T> returnPage(Page<T> pageInfo) {
        if (U.isNull(pageInfo)) {
            return PageReturn.empty();
        } else {
            return PageReturn.page(pageInfo.getTotalRow(), pageInfo.getRecords());
        }
    }

    public static <T,S> PageReturn<T> returnPage(Page<S> page, Class<T> clazz) {
        if (U.isNull(page)) {
            return PageReturn.empty();
        }

        long total = page.getTotalRow();
        if (U.less0(total)) {
            return PageReturn.empty();
        }

        List<S> objList = page.getRecords();
        if (A.isEmpty(objList)) {
            return PageReturn.page(total, Collections.emptyList());
        }

        List<T> list = JsonUtil.convertList(objList, clazz);
        return PageReturn.page(total, A.isEmpty(list) ? Collections.emptyList() : list);
    }
}
