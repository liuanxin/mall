package com.github.common.page;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.List;

/**
 * <pre>
 * 此实体类只在 <span style="color:red">Service</span> 中用到分页时使用.
 *
 * &#064;Controller --> request 请求中带过来的参数使用 Page 进行接收(如果前端不传, 此处接收则程序会使用默认值)
 * public JsonResult xx(xxx, Page page) {
 *     PageInfo pageInfo = xxxService.page(xxx, page);
 *     return success("xxx", (page.isWasMobile() ? pageInfo.getList() : pageInfo));
 * }
 *
 * &#064;Service --> 调用方法使用 Page 进行传递, 返回 PageInfo
 * public PageInfo page(xxx, Page page) {
 *    QueryWrapper<UserTest> queryWrapper = Wrappers.lambdaQuery(XXX.class).select(XXX::getId, XXX::getName ...);
 *     if (U.isNotBlank(xxx)) {
 *         query.eq(U.isNotBlank(xxx.getType()), ProductTest::getType, param.getType().getCode());
 *         query.eq(U.isNotBlank(xxx.getName()), ProductTest::getName, U.rightLike(param.getName()));
 *     }
 *     // 会生成 select id, name from xxx where type
 *     return Pages.returnPage(xxxMapper.selectPage(xxxxx, Pages.param(page), queryWrapper));
 * }
 * </pre>
 */
public final class Pages {

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换 */
    public static <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> paramOnlyLimit(long limit) {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit, false);
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换 */
    public static <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> param(Page page) {
        return page.isWasMobile()
                ? paramOnlyLimit(page.getLimit())
                : new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page.getPage(), page.getLimit());
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的返回类型是 List, service 上的返回类型是 PageInfo, 使用此方法进行转换 */
    public static <T> PageInfo<T> returnPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> pageObj) {
        if (U.isBlank(pageObj)) {
            return PageInfo.emptyReturn();
        } else {
            List<T> objList = pageObj.getRecords();
            return A.isEmpty(objList) ? PageInfo.emptyReturn() : PageInfo.returnPage(pageObj.getTotal(), objList);
        }
    }
}
