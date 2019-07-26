package com.github.web;

import com.github.common.enums.TestEnum;
import com.github.common.json.JsonResult;
import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.dto.ExampleDto;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.product.constant.ProductConst;
import com.github.product.model.ProductTest;
import com.github.product.service.ProductTestService;
import com.github.user.model.UserTest;
import com.github.user.service.UserTestService;
import com.github.vo.ExampleVo;
import com.google.common.collect.Lists;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiGroup(ProductConst.MODULE_INFO)
@RestController
@RequestMapping("/product")
public class BackendExampleController {

    private final UserTestService userExampleService;
    private final ProductTestService productExampleService;

    public BackendExampleController(UserTestService userExampleService, ProductTestService productExampleService) {
        this.userExampleService = userExampleService;
        this.productExampleService = productExampleService;
    }

    @ApiMethod(value = "分页查询", develop = Develop.PRODUCT)
    @GetMapping
    public JsonResult<PageInfo<ExampleVo>> enumList(ExampleDto dto,
                                                    @ApiParam(value = "枚举", example = "One") TestEnum enumTest,
                                                    Page page) {
        U.assertNil(dto, "缺少必须的参数");
        dto.basicCheck();

        PageInfo<UserTest> pageUserExampleInfo = userExampleService.example(dto.userParam(), page);
        List<Long> userIdList = Lists.newArrayList();
        if (U.isNotBlank(pageUserExampleInfo)) {
            if (A.isNotEmpty(pageUserExampleInfo.getList())) {
                for (UserTest userExample : pageUserExampleInfo.getList()) {
                    userIdList.add(userExample.getId());
                }
            }
        }
        List<ProductTest> productExampleList = productExampleService.example(userIdList, dto.productParam());
        return JsonResult.success("示例", ExampleVo.assemblyData(pageUserExampleInfo, productExampleList));
    }
}
