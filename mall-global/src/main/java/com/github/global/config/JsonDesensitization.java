package com.github.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class JsonDesensitization {

    /** 是否进行脱敏, 默认进行脱敏 */
    @Value("${json.hasDesensitization:true}")
    private boolean hasDesensitization;

    /** 是否进行数据压缩, 默认不压缩 */
    @Value("${json.hasCompress:false}")
    private boolean hasCompress;


    private final ObjectMapper objectMapper;
    private final ObjectMapper desObjectMapper;

    public JsonDesensitization(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.desObjectMapper = objectMapper.copy();
        this.desObjectMapper.registerModule(new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                // null 不序列化
                if (U.isNull(value)) {
                    return;
                }
                // 空白符直接返回
                if (U.EMPTY.equals(value.trim())) {
                    gen.writeString(value);
                    return;
                }

                // 脱敏字段
                String key = gen.getOutputContext().getCurrentName();
                if (U.isNotBlank(key)) {
                    switch (key.toLowerCase()) {
                        case "password":
                            gen.writeString("***");
                            return;
                        case "phone":
                            gen.writeString(U.foggyPhone(value));
                            return;
                        case "id_card":
                        case "id-card":
                        case "idcard":
                            gen.writeString(U.foggyIdCard(value));
                            return;
                    }
                }
                gen.writeString(value);
            }
        }));
    }

    public String toJson(Object data) {
        if (U.isNull(data)) {
            return U.EMPTY;
        }

        try {
            String json = (hasDesensitization ? desObjectMapper : objectMapper).writeValueAsString(data);
            if (hasCompress && U.isNotBlank(json)) {
                json = U.compress(json);
            }
            return json;
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("data desensitization exception", e);
            }
            return U.EMPTY;
        }
    }
}
