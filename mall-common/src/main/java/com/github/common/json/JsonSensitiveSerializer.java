package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.reflect.Field;

/** 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在业务接口上, 在需要做脱敏的  */
public class JsonSensitiveSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider s) throws IOException {
        if (U.isNull(value)) {
            gen.writeNull();
            return;
        }
        if (U.isBlank(value)) {
            gen.writeString(U.EMPTY);
            return;
        }

        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        if (U.isNotNull(field)) {
            JsonSensitive sensitive = field.getAnnotation(JsonSensitive.class);
            if (U.isNotNull(sensitive)) {
                int start = Math.max(0, sensitive.start());
                int end = Math.max(0, sensitive.end());
                int length = value.length();

                StringBuilder sbd = new StringBuilder();
                if (start > 0 && start < length) {
                    sbd.append(value, 0, start);
                }
                sbd.append(" ***");
                if (end > 0 && length > (start + end + 5)) {
                    sbd.append(" ").append(value, length - end, length);
                }
                String text = sbd.toString();
                if (U.isNotBlank(text)) {
                    gen.writeString(text);
                    return;
                }
            }
        }

        gen.writeString(value);
    }
}
