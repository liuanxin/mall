package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.common.date.DateUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public final class JsonModule {

    /** 全局序列化反序列化模块 */
    public static final SimpleModule GLOBAL_MODULE = new SimpleModule()
            .addSerializer(Integer.TYPE, ToStringSerializer.instance)
            .addSerializer(Integer.class, ToStringSerializer.instance)
            .addSerializer(Long.class, ToStringSerializer.instance)
            .addSerializer(Long.TYPE, ToStringSerializer.instance)
            .addSerializer(Float.class, ToStringSerializer.instance)
            .addSerializer(Float.TYPE, ToStringSerializer.instance)
            .addSerializer(Double.class, ToStringSerializer.instance)
            .addSerializer(Double.TYPE, ToStringSerializer.instance)
            .addSerializer(BigDecimal.class, new BigDecimalSerializer())

            .addDeserializer(BigDecimal.class, new BigDecimalDeserializer())
            .addDeserializer(Date.class, new DateDeserializer());


    /** 脱敏用到的序列化模块 */
    public static final SimpleModule DES_MODULE = new SimpleModule().addSerializer(String.class, new StringDesensitization());


    /** 字符串脱敏 */
    public static class StringDesensitization extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (U.isNull(value)) {
                gen.writeString(U.EMPTY);
                return;
            }
            String key = gen.getOutputContext().getCurrentName();
            if (U.isBlank(key)) {
                gen.writeString(value);
                return;
            }

            switch (key.toLowerCase()) {
                case "password":
                    gen.writeString("***");
                    return;
                case "phone":
                    gen.writeString(U.foggyPhone(value));
                    return;
                case "idcard":
                case "id-card":
                case "id_card":
                    gen.writeString(U.foggyIdCard(value));
                    return;
            }

            int valueLen = value.length(), max = 1000, len = 200;
            if (valueLen > max) {
                gen.writeString(value.substring(0, len) + " *** " + value.substring(valueLen - len));
                return;
            }

            gen.writeString(value);
        }
    }

    /** 序列化 BigDecimal 小数位不足 2 位的返回 2 位 */
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

    // ================= 上面是序列化, 下面是反序列化 =================

    /** 反序列化 Date, 序列化使用全局配置, 或者属性上的 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 注解 */
    public static class DateDeserializer extends JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            Date date = DateUtil.parse(p.getText().trim());
            return (U.isNotNull(date) && date.getTime() == 0) ? null : date;
        }
    }

    /** 反序列化 BigDecimal */
    public static class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {
        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String text = p.getText();
            return U.isBlank(text) ? null : new BigDecimal(text.trim());
        }
    }
}
