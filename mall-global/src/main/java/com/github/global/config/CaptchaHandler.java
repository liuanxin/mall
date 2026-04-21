package com.github.global.config;

import com.github.common.util.Arr;
import com.github.common.util.CaptchaUtil;
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

    public void saveChallenge(String id, CaptchaUtil.CaptchaChallenge challenge) {
        Cache cache = cacheManager.getCache(CAPTCHA_CHALLENGE_CACHE);
        if (cache != null) {
            cache.put(id, challenge);
        }
    }

    public String verifyAndIssuePassToken(String id, List<Map<String, Double>> points) {
        Cache cache = cacheManager.getCache(CAPTCHA_CHALLENGE_CACHE);
        if (cache == null) {
            return null;
        }
        CaptchaUtil.CaptchaChallenge challenge = cache.get(id, CaptchaUtil.CaptchaChallenge.class);
        if (challenge == null) {
            return null;
        }
        cache.evict(id);

        List<CaptchaUtil.CaptchaPointInput> inputList = new ArrayList<>();
        if (Arr.isNotEmpty(points)) {
            for (Map<String, Double> point : points) {
                if (point == null) {
                    return null;
                }
                double x = point.getOrDefault("x", -1D);
                double y = point.getOrDefault("y", -1D);
                inputList.add(new CaptchaUtil.CaptchaPointInput(x, y));
            }
        }
        boolean pass = CaptchaUtil.verifyClick(challenge, inputList, null);
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
