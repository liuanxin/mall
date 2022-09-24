package com.github;

import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class WebManagerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebManagerApplication.class);
    }

    public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        ConfigurableApplicationContext ctx = SpringApplication.run(WebManagerApplication.class, args);
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            String profile = A.toStr(ctx.getEnvironment().getActiveProfiles());
            String appName = ctx.getEnvironment().getProperty("spring.application.name");
            String port = ctx.getEnvironment().getProperty("server.port");
            String time = DateUtil.toHuman(System.currentTimeMillis() - ms);
            LogUtil.ROOT_LOG.info("run({}) profile({}), port({}), use time({})", appName, profile, port, time);
        }
    }
}
