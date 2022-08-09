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
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

public final class JsonModule {

    /** 全局序列化反序列化模块 */
    public static final SimpleModule GLOBAL_MODULE = new SimpleModule()
            // Long 或 long 序列化时用 String 类型, 避免前端处理 long 时精度丢失
            .addSerializer(Long.class, ToStringSerializer.instance)
            .addSerializer(long.class, ToStringSerializer.instance)
            .addSerializer(BigDecimal.class, BigDecimalSerializer.INSTANCE)

            .addDeserializer(BigDecimal.class, BigDecimalDeserializer.INSTANCE)
            .addDeserializer(Date.class, DateDeserializer.INSTANCE);


    /** 日志脱敏用到的序列化模块 */
    public static final SimpleModule LOG_SENSITIVE_MODULE = new SimpleModule()
            .addSerializer(String.class, GlobalLogSensitiveSerializer.INSTANCE);


    /** 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在日志打印上 */
    public static class GlobalLogSensitiveSerializer extends JsonSerializer<String> {
        public static final GlobalLogSensitiveSerializer INSTANCE = new GlobalLogSensitiveSerializer();

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(DesensitizationUtil.desByKey(gen.getOutputContext().getCurrentName(), value));
        }
    }

    /** 序列化 BigDecimal: 去掉尾部的 0(比如 10.00 --> 10, 比如 10.020 -> 10.02) */
    public static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        public static final BigDecimalSerializer INSTANCE = new BigDecimalSerializer();

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (U.isNull(value)) {
                gen.writeNull();
            } else {
                gen.writeString(value.stripTrailingZeros().toPlainString());
            }
        }
    }

    // ================= 上面是序列化, 下面是反序列化 =================

    /** 反序列化 Date. 注意: 序列化使用全局配置, 或者属性上的 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 注解 */
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
