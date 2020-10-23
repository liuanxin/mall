package com.github.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.common.util.A;
import com.github.common.util.U;

import java.util.List;

/**
 * <pre>
 * 此实体类在 Controller 和 Service 中用到分页时使用.
 *
 * &#064;Controller --> request 请求中带过来的参数使用 Page 进行接收(如果前端不传, 此处接收则程序会使用默认值)
 * public JsonResult xx(xxx, PageParam page) {
 *     PageReturn pageInfo = xxxService.page(xxx, page);
 *     return success("xxx", (page.isWasMobile() ? pageInfo.getList() : pageInfo));
 * }
 *
 * &#064;Service --> 调用方法使用 Page 进行传递, 返回 PageInfo
 * public PageReturn page(xxx, PageParam page) {
 *     List&lt;XXX> xxxList = xxxMapper.selectPage(Pages.param(page), xxxxx);
 *     return Pages.returnPage(xxxList);
 * }
 *
 * 这么做的目的是分页包只需要在服务端引入即可
 * </pre>
 */
public final class Pages {

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换 */
    public static <T> Page<T> paramOnlyLimit(long limit) {
        return new Page<>(1, limit, false);
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换 */
    public static <T> Page<T> param(PageParam page) {
        return page.isWasMobile()
                ? paramOnlyLimit(page.getLimit())
                : new Page<>(page.getPage(), page.getLimit());
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的返回类型是 List, service 上的返回类型是 PageInfo, 使用此方法进行转换 */
    public static <T> PageReturn<T> returnPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> pageObj) {
        if (U.isBlank(pageObj)) {
            return PageReturn.emptyReturn();
        } else {
            List<T> objList = pageObj.getRecords();
            return A.isEmpty(objList) ? PageReturn.emptyReturn() : PageReturn.returnPage(pageObj.getTotal(), objList);
        }
    }
}
