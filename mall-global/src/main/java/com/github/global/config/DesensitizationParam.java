package com.github.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.util.LogUtil;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class DesensitizationParam {

    /** 需要脱敏的字段, 比如用户传入 password 将输出成 *** */
    private static final Set<String> DESENSITIZATION_FIELD_SET = Sets.newHashSet("password");
    /** 单个字符串的长度超出此值则进行脱敏, 只输出前后. 如 max 是 5, left_right 是 1, 当输入「abcde」将输出成「a ... e」 */
    private static final int SINGLE_FIELD_MAX_LENGTH = 1000;
    /** 单个字符串的长度脱敏时, 只输出前后的长度. 如 max 是 5, left_right 是 1, 当输入「abcde」将输出成「a ... e」 */
    private static final int SINGLE_FIELD_LEFT_RIGHT_LENGTH = 200;

    private final ObjectMapper desensitizationMapper;

    public DesensitizationParam(ObjectMapper objectMapper) {
        this.desensitizationMapper = objectMapper.copy();
        this.desensitizationMapper.registerModule(new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeString(value);
                    return;
                }

                if (DESENSITIZATION_FIELD_SET.contains(gen.getOutputContext().getCurrentName().toLowerCase())) {
                    gen.writeString("***");
                    return;
                }

                int len = value.length();
                if (len >= SINGLE_FIELD_MAX_LENGTH) {
                    String left = value.substring(0, SINGLE_FIELD_LEFT_RIGHT_LENGTH);
                    String right = value.substring(len - SINGLE_FIELD_LEFT_RIGHT_LENGTH, len);
                    gen.writeString(left + " ... " + right);
                    return;
                }
                gen.writeString(value);
            }
        }));
    }

    public String handleDesensitization(Object json) {
        try {
            return desensitizationMapper.writeValueAsString(json);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("desensitization json data exception", e);
            }
            return null;
        }
    }
}
