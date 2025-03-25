package com.github;

import com.github.common.date.Dates;
import com.github.common.util.LogUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class WebBackendApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebBackendApplication.class);
    }

    public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        ConfigurableApplicationContext ctx = SpringApplication.run(WebBackendApplication.class, args);
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            String profile = String.join(",", ctx.getEnvironment().getActiveProfiles());
            String appName = ctx.getEnvironment().getProperty("spring.application.name");
            String port = ctx.getEnvironment().getProperty("server.port");
            String time = Dates.toHuman(System.currentTimeMillis() - ms);
            LogUtil.ROOT_LOG.info("run({}) profile({}), port({}), use time({})", appName, profile, port, time);
        }
    }
}
