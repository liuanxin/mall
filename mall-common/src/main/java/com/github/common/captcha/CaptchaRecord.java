package com.github.common.captcha;

import java.util.List;

/**
 * 点选验证码共享模型.
 */
public final class CaptchaRecord {

    private CaptchaRecord() {
    }

    /** 图内坐标点(像素) */
    public record Point(int x, int y) {
    }

    /** 单个字形元数据(点击命中用) */
    public record Glyph(String value, int x, int y, int radius, boolean target, int targetOrder) {
    }

    /** 挑战(服务端缓存, 校验点击用) */
    public record Challenge(
            List<String> targetChars,
            List<Glyph> glyphList,
            int width,
            int height,
            int clickAreaTop
    ) {
    }

    /** 生成结果(图 data URI + 挑战) */
    public record Build(String image, int width, int height, Challenge challenge) {
    }

    /** 前端点击点(相对坐标 0~1) */
    public record PointInput(double x, double y) {
    }
}
