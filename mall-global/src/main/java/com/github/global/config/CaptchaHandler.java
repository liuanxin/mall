package com.github.global.config;

import com.github.common.captcha.Captcha;
import com.github.common.captcha.CaptchaPngUtil;
import com.github.common.captcha.CaptchaSvgPathUtil;
import com.github.common.captcha.CaptchaSvgUtil;
import com.github.common.util.Arr;
import com.github.common.util.Obj;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class CaptchaHandler {

    private static final String CAPTCHA_CHALLENGE_CACHE = "captcha_challenge";
    private static final String CAPTCHA_PASS_CACHE = "captcha_pass";

    private final CacheManager cacheManager;

    /**
     * @param png true 用 PNG; 否则 SVG(有中文字体走 Path, 否则 text SVG)
     */
    public Captcha.Build saveChallenge(String id, String width, String height, String dark, String png) {
        Captcha.Build build;
        if (Obj.toBool(png)) {
            build = CaptchaPngUtil.buildClickCaptcha(width, height, dark);
        } else {
            if (Captcha.hasCjkFonts()) {
                build = CaptchaSvgPathUtil.buildClickCaptcha(width, height, dark);
            } else {
                build = CaptchaSvgUtil.buildClickCaptcha(width, height, dark);
            }
        }
        Cache cache = cacheManager.getCache(CAPTCHA_CHALLENGE_CACHE);
        if (cache != null) {
            cache.put(id, build.challenge());
        }
        return build;
    }

    public String verifyAndIssuePassToken(String id, List<Map<String, Double>> points) {
        Cache cache = cacheManager.getCache(CAPTCHA_CHALLENGE_CACHE);
        if (cache == null) {
            return null;
        }
        Captcha.Challenge challenge = cache.get(id, Captcha.Challenge.class);
        if (challenge == null) {
            return null;
        }
        cache.evict(id);

        List<Captcha.PointInput> inputList = new ArrayList<>();
        if (Arr.isNotEmpty(points)) {
            for (Map<String, Double> point : points) {
                if (point == null) {
                    return null;
                }
                double x = point.getOrDefault("x", -1D);
                double y = point.getOrDefault("y", -1D);
                inputList.add(new Captcha.PointInput(x, y));
            }
        }
        boolean pass = Captcha.verifyClick(challenge, inputList, null);
        if (!pass) {
            return null;
        }
        String passToken = Obj.uuid16();
        Cache passCache = cacheManager.getCache(CAPTCHA_PASS_CACHE);
        if (passCache != null) {
            passCache.put(passToken, "1");
            return passToken;
        }
        return null;
    }

    public boolean consumePassToken(String passToken) {
        Cache passCache = cacheManager.getCache(CAPTCHA_PASS_CACHE);
        if (passCache == null) {
            return false;
        }
        String value = passCache.get(passToken, String.class);
        if (Obj.isBlank(value)) {
            return false;
        }
        passCache.evict(passToken);
        return true;
    }
}
