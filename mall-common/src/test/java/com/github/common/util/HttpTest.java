package com.github.common.util;

import com.github.common.http.ApacheHttpClientUtil;
import com.github.common.http.HttpClientUtil;
import com.github.common.http.HttpUrlConnectionUtil;
import com.github.common.http.OkHttpClientUtil;

public class HttpTest {

    public static void main(String[] args) {
        String url = "http://weibo.com";

        OkHttpClientUtil.get(url);
        System.out.println("okhttp");
        System.out.println("\n-----");

        ApacheHttpClientUtil.get(url);
        System.out.println("Apache-HttpClient");
        System.out.println("\n-----");

        HttpUrlConnectionUtil.get(url);
        System.out.println("jdk-HttpUrlConnection");
        System.out.println("\n-----");

        HttpClientUtil.get(url);
        System.out.println("jdk-HttpClient");
        System.out.println("\n-----");
    }
}
