package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.date.DateUtil;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public final class JsonModule {

    /** 全局序列化反序列化模块 */
    public static final SimpleModule GLOBAL_MODULE = new SimpleModule()
            .addSerializer(BigDecimal.class, BigDecimalSerializer.INSTANCE)

            .addDeserializer(BigDecimal.class, BigDecimalDeserializer.INSTANCE)
            .addDeserializer(Date.class, DateDeserializer.INSTANCE);


    /** 脱敏用到的序列化模块 */
    public static final SimpleModule DES_MODULE = new SimpleModule()
            .addSerializer(String.class, StringDesensitization.instance);


    /** 字符串脱敏 */
    public static class StringDesensitization extends JsonSerializer<String> {
        public static final StringDesensitization instance = new StringDesensitization();

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (U.isNull(value)) {
                gen.writeNull();
                return;
            }
            if (U.isBlank(value)) {
                gen.writeString(U.EMPTY);
                return;
            }

            String fieldName = gen.getOutputContext().getCurrentName();
            try {
                Field field = U.getFieldInfo(gen.getCurrentValue(), fieldName);
                if (U.isNotNull(field)) {
                    JsonSensitive sensitive = field.getAnnotation(JsonSensitive.class);
                    if (U.isNotNull(sensitive)) {
                        int length = value.length();
                        int start = Math.max(0, sensitive.start());
                        int end = Math.min(length, sensitive.end());

                        StringBuilder sbd = new StringBuilder();
                        if (start > 0 && start < length) {
                            sbd.append(value, 0, start).append(" ***");
                        }
                        if (end > 0 && end > start && end < length) {
                            sbd.append(" ").append(value, end, length);
                        }
                        String text = sbd.toString();
                        if (U.isNotBlank(text)) {
                            gen.writeString(text);
                            return;
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            gen.writeString(DesensitizationUtil.des(fieldName, value));
        }
    }

    /** 序列化 BigDecimal 小数位不足 2 位的返回 2 位 */
    public static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        public static final BigDecimalSerializer INSTANCE = new BigDecimalSerializer();

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            int minScale = 2;
            if (U.isNull(value)) {
                gen.writeNull();
            } else if (value.scale() < minScale) {
                // 忽略小数位后的值, 有值就进 1 则使用 RoundingMode.UP
                gen.writeString(value.setScale(minScale, RoundingMode.DOWN).toString());
            } else {
                gen.writeString(value.toString());
            }
        }
    }

    // ================= 上面是序列化, 下面是反序列化 =================

    /** 反序列化 Date, 序列化使用全局配置, 或者属性上的 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 注解 */
    public static class DateDeserializer extends JsonDeserializer<Date> {
        public static final DateDeserializer INSTANCE = new DateDeserializer();

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            Date date = DateUtil.parse(p.getText().trim());
            return (U.isNotNull(date) && date.getTime() == 0) ? null : date;
        }
    }

    /** 反序列化 BigDecimal */
    public static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
        public static final BigDecimalDeserializer INSTANCE = new BigDecimalDeserializer();

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            return U.isBlank(text) ? null : new BigDecimal(text.trim());
        }
    }
}
