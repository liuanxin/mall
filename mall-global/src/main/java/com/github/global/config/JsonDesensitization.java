package com.github.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class JsonDesensitization {

    /** 需要脱敏的字段, 比如用户传入 password 将输出成 *** */
    private static final Set<String> DESENSITIZATION_FIELD_SET = Sets.newHashSet("password");
    /** 单个字符串的长度超出此值则进行脱敏, 只输出前后. 如 max 是 5, left_right 是 1, 当输入「abcde」将输出成「a ... e」 */
    private static final int SINGLE_FIELD_MAX_LENGTH = 500;
    /** 单个字符串只输出前后的长度. 如 max 是 5, left_right 是 1, 当输入「abcde」将输出成「a ... e」 */
    private static final int SINGLE_FIELD_LEFT_RIGHT_LENGTH = 100;

    /** 总字符串的长度超出此值则进行脱敏, 只输出前后 */
    private static final int STRING_MAX_LENGTH = 2000;
    /** 总字符串只输出前后的长度 */
    private static final int STRING_LEFT_RIGHT_LENGTH = 400;

    private final ObjectMapper desensitizationMapper;

    public JsonDesensitization(ObjectMapper objectMapper) {
        this.desensitizationMapper = objectMapper.copy();
        this.desensitizationMapper.registerModule(new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                // null 不序列化
                if (U.isNull(value)) {
                    return;
                }
                // 空字符直接返回
                if ("".equals(value)) {
                    gen.writeString("");
                    return;
                }
                // 脱敏字段
                if (DESENSITIZATION_FIELD_SET.contains(gen.getOutputContext().getCurrentName().toLowerCase())) {
                    gen.writeString("***");
                    return;
                }
                // 过长的字段只输出前后字符
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

    public String handle(Object data) {
        if (U.isNull(data)) {
            return null;
        }
        try {
            String json = desensitizationMapper.writeValueAsString(data);
            if (U.isNotBlank(json)) {
                int len = json.length();
                if (len >= STRING_MAX_LENGTH) {
                    String left = json.substring(0, STRING_LEFT_RIGHT_LENGTH);
                    String right = json.substring(len - STRING_LEFT_RIGHT_LENGTH, len);
                    json = left + " ... " + right;
                }
            }
            return json;
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("desensitization data data exception", e);
            }
            return null;
        }
    }
}
