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
            String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
            if (A.isNotEmpty(activeProfiles)) {
                LogUtil.ROOT_LOG.debug("current profile : ({})", A.toStr(activeProfiles));
            }
        }
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("运行成功, 耗时({})", DateUtil.toHuman(System.currentTimeMillis() - ms));
        }
    }
}
