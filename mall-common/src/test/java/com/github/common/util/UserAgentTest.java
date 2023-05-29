package com.github.common.util;

import com.github.common.json.JsonUtil;
import com.github.common.ua.UserAgent;
import com.github.common.ua.UserAgentUtil;

import java.util.Arrays;
import java.util.List;

public class UserAgentTest {

    public static void main(String[] args) {
        List<String> list = Arrays.asList(
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1",
                "Mozilla/5.0 (Linux; Android 10; ELS-AN00; HMSCore 5.1.0.309) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 HuaweiBrowser/11.0.6.302 Mobile Safari/537.36",
                "Dalvik/2.1.0 (Linux; U; Android 10; ELS-AN00 Build/HUAWEIELS-AN00)",

                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36",
                "Mozilla/5.0 (BlackBerry; U; BlackBerry 9900; en) AppleWebKit/534.11+ (KHTML, like Gecko) Version/7.1.0.346 Mobile Safari/534.11+",

                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Weibo(iPhone12,1__weibo__11.8.2__iphone__os14.4.2)",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Weibo(iPhone11,6__weibo__13.4.3__iphone__os15.4.1)",
                "Mozilla/5.0 (Linux; Android 11; PDAM10 Build/RKQ1.200903.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 MobileSafari/537.36",
                "Mozilla/5.0 (Linux; Android 10; V2057A Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/107.0.5304.141 Mobile Safari/537.36 XWEB/5075 MMWEBSDK/20230405 MMWEBID/3511 MicroMessenger/8.0.35.2360(0x2800235B) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64 MiniProgramEnv/android"
        );
        for (String str : list) {
            UserAgent ua = UserAgentUtil.parse(str);
            System.out.println(ua + "\n" + JsonUtil.toJson(ua) + "\n");
        }
    }
}
