package com.github.req;

import com.github.common.util.Obj;
import com.github.liuanxin.api.annotation.ApiParam;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CaptchaVerifyReq {

    @ApiParam("挑战 id")
    private String id;

    @ApiParam("""
            点击列表 [{"x": 0.1, "y": 0.2},{"x": 0.6, "y": 0.7}]
            """)
    private List<Map<String, Double>> points;

    public void validate() {
        Obj.assertBlank(id, "验证码 id 不能为空");
        Obj.assertEmpty(points, "请完成验证码点击");
    }
}
