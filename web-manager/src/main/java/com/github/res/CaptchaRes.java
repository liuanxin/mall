package com.github.res;

import com.github.common.util.CaptchaUtil;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaRes {

    @ApiReturn("挑战 id")
    private String id;

    @ApiReturn("验证码图 base64")
    private String base64;

    @ApiReturn("点击提示文案")
    private String text;

    @ApiReturn("图宽度")
    private Integer width;

    @ApiReturn("图高度")
    private Integer height;

    public static CaptchaRes assembly(String id, CaptchaUtil.CaptchaBuild build) {
        return new CaptchaRes(id, build.base64(), build.text(), build.width(), build.height());
    }
}
