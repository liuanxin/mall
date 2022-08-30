package com.github.common.util;

import com.github.common.http.*;

public class HttpTest {

    public static void main(String[] args) {
        String url = "https://weibo.com";

        ResponseData<String> res;
        res = OkHttpClientUtil.get(url);
        String okhttp = res.getData().trim();
        System.out.println("\n-----");
        res = ApacheHttpClientUtil.get(url);
        String apache = res.getData().trim();
        System.out.println("\n-----");
        res = HttpUrlConnectionUtil.get(url);
        String connection = res.getData().trim();
        System.out.println("\n-----");
        res = HttpClientUtil.get(url);
        String httpclient = res.getData().trim();
        System.out.println("\n-----");
    }
}
