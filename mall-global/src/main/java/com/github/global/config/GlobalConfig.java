package com.github.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.date.DateUtil;
import com.github.common.util.ApplicationContexts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    @ConditionalOnClass(ObjectMapper.class)
    public static class JsonModule {

        @JsonComponent
        public static class DataJsonModule {
            public static class Serializer extends JsonDeserializer<Date> {
                @Override
                public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                    return DateUtil.parse(p.getText().trim());
                }
            }
        }

        /*@Bean
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
}
