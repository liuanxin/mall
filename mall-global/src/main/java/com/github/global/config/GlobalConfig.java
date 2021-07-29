package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.json.JsonModule;
import com.github.common.util.ApplicationContexts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class GlobalConfig {

    @Bean
    public ApplicationContexts setupApplicationContext() {
        return new ApplicationContexts();
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        objectMapper.registerModule(JsonModule.dateDeserializer());
        objectMapper.registerModule(JsonModule.bigDecimalSerializer());
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    // /** see: https://www.baeldung.com/spring-boot-customize-jackson-objectmapper */
    /*@Configuration
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

    @Bean
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
