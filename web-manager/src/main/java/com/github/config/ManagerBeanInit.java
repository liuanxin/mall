package com.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 项目中需要额外加载的类 */
@Configuration
public class ManagerBeanInit {

    @Bean
    @ConfigurationProperties(prefix = "config")
    public ManagerConfig config() {
        return new ManagerConfig();
    }
}
