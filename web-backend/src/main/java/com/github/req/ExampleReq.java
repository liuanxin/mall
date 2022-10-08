package com.github.req;

import com.github.common.enums.Gender;
import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.product.enums.ProductTestType;
import com.github.product.model.ProductTest;
import com.github.user.enums.UserTestLevel;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExtend;
import lombok.Data;

@Data
public class ExampleReq {

    @ApiParam("昵称")
    private String nickName;

    @ApiParam(value = "性别", required = true, example = "1")
    private Gender gender;

    @ApiParam("用户等级")
    private UserTestLevel level;

    // 上面的属性跟 user 有关, 下面的属性跟 product 有关

    @ApiParam("商品名(60 个字以内)")
    private String name;

    @ApiParam(value = "商品类型", example = "2")
    private ProductTestType type;

    /** 基本的数据检查 */
    public void basicCheck() {
        if (U.isNotBlank(nickName)) {
            int max = 30;
            U.assertException(nickName.length() > max, String.format("昵称只在 %s 位以内", max));
        }
        U.assertNil(gender, "请选择性别进行查询");
        // 检查还是给默认值, 看业务
        if (U.isNull(level)) {
            level = UserTestLevel.Normal;
        }

        if (U.isNotBlank(name)) {
            int max = 60;
            U.assertException(name.length() > max, String.format("商品名只在 %s 位以内", max));
        }
    }

    /** 操作用户模块时用到的参数 */
    public UserTest userTestParam() {
        return JsonUtil.convert(this, UserTest.class);
    }
    public UserTestExtend userTestExtendParam() {
        return JsonUtil.convert(this, UserTestExtend.class);
    }
    /** 操作商品模块时需要的参数 */
    public ProductTest productParam() {
        return JsonUtil.convert(this, ProductTest.class);
    }
}
