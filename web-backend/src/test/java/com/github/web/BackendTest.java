package com.github.web;

import com.github.common.http.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest (webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BackendTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String prefixUrl;

    @Before
    public void setup() {
        prefixUrl = "http://127.0.0.1:" + port;
    }

    @Test
    public void test() {
        ResponseEntity<String> res = restTemplate.getForEntity("/", String.class);
        System.out.println("test1: " + res);

        System.out.println("\n=================");

        ResponseEntity<String> response = restTemplate.getForEntity(prefixUrl + "/", String.class);
        System.out.println("test2: " + response);

        System.out.println("\n-----------------");

        System.out.println("http test1: " + HttpClientUtil.get(prefixUrl + "/"));
    }
}