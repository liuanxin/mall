package com.github;

import com.github.common.date.DateUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class WebBackendApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebBackendApplication.class);
    }

    public static void main(String[] args) {
        long ms = System.currentTimeMillis();
        ApplicationContext ctx = new SpringApplicationBuilder().sources(WebBackendApplication.class).run(args);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.info("run success, current profile({}), use time({})",
                    A.toStr(ctx.getEnvironment().getActiveProfiles()), DateUtil.toHuman(System.currentTimeMillis() - ms));
        }
    }
}
