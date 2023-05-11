package com.github.web;

import com.github.common.json.JsonResult;
import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.common.util.U;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.product.constant.ProductConst;
import com.github.product.model.ProductTest;
import com.github.req.ExampleReq;
import com.github.res.ExampleRes;
import com.github.user.model.UserTest;
import com.github.user.service.UserTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiGroup(value = ProductConst.MODULE_INFO, index = 3)
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class BackendProductController {

    private final UserTestService userExampleService;

    @ApiMethod(value = "分页查询", develop = Develop.PRODUCT)
    @GetMapping
    public JsonResult<PageReturn<ExampleRes>> enumList(ExampleReq req, PageParam page) {
        U.assertNil(req, "缺少必须的参数");
        req.basicCheck();

        PageReturn<UserTest> pageUserExampleInfo = userExampleService.example(req.userTestParam(), req.userTestExtendParam(), page);
        /*
        List<Long> userIdList = new ArrayList<>();
        if (U.isNotBlank(pageUserExampleInfo)) {
            if (A.isNotEmpty(pageUserExampleInfo.getList())) {
                for (UserTest userExample : pageUserExampleInfo.getList()) {
                    userIdList.add(userExample.getId());
                }
            }
        }
        */
        List<ProductTest> productExampleList = null;// productExampleService.example(userIdList, req.productParam());
        return JsonResult.success("示例", ExampleRes.assemblyData(pageUserExampleInfo, productExampleList));
    }
}
