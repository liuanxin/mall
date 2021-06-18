package com.github.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.date.DateUtil;
import com.github.common.util.ApplicationContexts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Date;

@Configuration
public class GlobalConfig {

    @Bean
    public ApplicationContexts setupApplicationContext() {
        return new ApplicationContexts();
    }

    /** see: https://www.baeldung.com/spring-boot-customize-jackson-objectmapper */
    @Configuration
    @ConditionalOnBean(ObjectMapper.class)
    public static class JsonCustomModule {
        @JsonComponent
        public static class CustomJsonComponent {
            public static class JsonDataDeserializer extends JsonDeserializer<Date> {
                @Override
                public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                    return DateUtil.parse(p.getText().trim());
                }
            }
        }
    }

    /*@Bean
    @ConditionalOnBean(ObjectMapper.class)
    public Module customModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return DateUtil.parse(p.getText().trim());
            }
        });
        return module;
    }*/
}
