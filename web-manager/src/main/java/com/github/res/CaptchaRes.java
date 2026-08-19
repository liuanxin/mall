package com.github.res;

import com.github.common.captcha.CaptchaRecord;
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

    @ApiReturn("验证码图(data URI, 可直接放入 img src)")
    private String image;

    @ApiReturn("图宽度")
    private Integer width;

    @ApiReturn("图高度")
    private Integer height;

    @ApiReturn("需要点击的目标字数")
    private Integer count;

    public static CaptchaRes assembly(String id, CaptchaRecord.Build build) {
        return new CaptchaRes(id, build.image(), build.width(), build.height(),
                build.challenge().targetChars().size());
    }
}
