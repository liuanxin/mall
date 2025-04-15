package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.json.JsonUtil;
import com.github.common.util.ApplicationContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
@SuppressWarnings("JavadocReference")
public class GlobalConfig {

    @Bean
    public ApplicationContexts setupApplicationContext() {
        return new ApplicationContexts();
    }

    /**
     * @see org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.JacksonObjectMapperConfiguration#jacksonObjectMapper
     * @see org.springframework.boot.autoconfigure.http.JacksonHttpMessageConvertersConfiguration.MappingJackson2HttpMessageConverterConfiguration#mappingJackson2HttpMessageConverter
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        // 加一些全局策略
        return JsonUtil.globalConfig(builder.createXmlMapper(false).build());
    }

    // /** see: https://www.baeldung.com/spring-boot-customize-jackson-objectmapper */
    /*
    @Configuration
    @ConditionalOnBean(ObjectMapper.class)
    @JsonComponent
    public static class JsonCustomModule {
        public static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (U.isNull(value)) {
                    gen.writeString(U.EMPTY);
                } else if (value.scale() < 2) {
                    gen.writeString(value.setScale(2, RoundingMode.DOWN).toString());
                } else {
                    gen.writeString(value.toString());
                }
            }
        }

        public static class DateDeserializer extends JsonDeserializer<Date> {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                Date date = DateUtil.parse(p.getText().trim());
                return (U.isNotNull(date) && date.getTime() == 0) ? null : date;
            }
        }
    }
    */

    /*
    @Bean
    @ConditionalOnBean(ObjectMapper.class)
    public Module customModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (U.isNull(value)) {
                    gen.writeString(U.EMPTY);
                } else if (value.scale() < 2) {
                    gen.writeString(value.setScale(2, RoundingMode.DOWN).toString());
                } else {
                    gen.writeString(value.toString());
                }
            }
        });

        module.addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return DateUtil.parse(p.getText().trim());
            }
        });
        return module;
    }
    */
}
