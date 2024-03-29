package com.github.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.enums.Gender;
import com.github.common.json.JsonUtil;
import com.github.common.page.PageReturn;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import com.github.product.enums.ProductTestType;
import com.github.product.model.ProductTest;
import com.github.user.model.UserTest;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExampleRes {

    @ApiReturn("用户 id")
    private Long id;

    @ApiReturn("昵称")
    private String nickName;

    @ApiReturn(value = "性别", example = "1")
    private Gender gender;

    @ApiReturn("用户头像")
    private String avatarUrl;

    @ApiReturn("用户的商品")
    private List<ExampleProductRes> exampleProductList;


    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ExampleProductRes {

        @ApiReturn("商品名")
        private String name;

        @ApiReturn("商品类型")
        private ProductTestType type;

        @ApiReturn("创建日期")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiReturn("最近更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;
    }


    /** 组装数据 */
    public static PageReturn<ExampleRes> assemblyData(PageReturn<UserTest> userPageInfo, List<ProductTest> testList) {
        PageReturn<ExampleRes> returnRes = PageReturn.convertTotal(userPageInfo);
        if (U.isNotNull(userPageInfo)) {
            // 把商品数据整理成  userId: List<商品>
            Map<Long, List<ProductTest>> userIdMap = A.listToMapList(testList, ProductTest::getUserId);

            List<ExampleRes> exampleVoList = new ArrayList<>();
            for (UserTest userExample : userPageInfo.getList()) {
                ExampleRes res = JsonUtil.convert(userExample, ExampleRes.class);
                if (U.isNotNull(res)) {
                    // 从上面的 map 中获取当前用户对应的商品列表
                    Collection<ProductTest> productExamples = userIdMap.get(userExample.getId());
                    if (A.isNotEmpty(productExamples)) {
                        List<ProductTest> examples = new ArrayList<>(productExamples);
                        // 把用户商品数据转换成前端需要的数据
                        res.setExampleProductList(JsonUtil.convertList(examples, ExampleProductRes.class));
                    } else {
                        // 如果没有商品数据也返回一个长度为 0 的数组
                        res.setExampleProductList(Collections.emptyList());
                    }
                    exampleVoList.add(res);
                }
            }
            returnRes.setList(exampleVoList);
        }
        return returnRes;
    }
}
