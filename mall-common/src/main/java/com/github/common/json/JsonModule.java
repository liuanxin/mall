package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonModule {

    /** 全局序列化反序列化模块 */
    public static final SimpleModule GLOBAL_MODULE = new SimpleModule()
            // Long 或 long 序列化时用 String 类型, 避免前端处理 long 时精度丢失
            .addSerializer(Long.class, ToStringSerializer.instance)
            .addSerializer(long.class, ToStringSerializer.instance)
            .addSerializer(BigDecimal.class, BigDecimalSerializer.INSTANCE)
            .addSerializer(Date.class, DateSerializer.INSTANCE) // com.fasterxml.jackson.databind.ser.std.DateSerializer
            .addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE)
            .addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE)

            .addDeserializer(BigDecimal.class, BigDecimalDeserializer.INSTANCE)
            .addDeserializer(Date.class, DateDeserializer.INSTANCE)
            .addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE)
            .addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE);


    /** 日志脱敏用到的序列化模块 */
    public static final SimpleModule LOG_SENSITIVE_MODULE = new SimpleModule()
            .addSerializer(String.class, GlobalLogSensitiveSerializer.INSTANCE);


    private static final Map<String, ? super Annotation> FIELD_FORMAT_MAP = new ConcurrentHashMap<>();
    private static JsonFormat getFormatField(Field field) {
        String key = field.getType().getName() + "#" + field.getName();
        JsonFormat format = (JsonFormat) FIELD_FORMAT_MAP.get(key);
        if (U.isNull(format)) {
            format = field.getAnnotation(JsonFormat.class);
            if (U.isNotNull(format)) {
                FIELD_FORMAT_MAP.put(key, format);
            }
        }
        return format;
    }


    /** 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在日志打印上 */
    public static class GlobalLogSensitiveSerializer extends JsonSerializer<String> {
        public static final GlobalLogSensitiveSerializer INSTANCE = new GlobalLogSensitiveSerializer();

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(DesensitizationUtil.desByKey(gen.getOutputContext().getCurrentName(), value));
        }
    }

    /** 序列化 Date: 将 1970-01-01 当成 null, 如果字段上有标 @JsonFormat 则用标的为主, 否则以默认方式处理 */
    public static class DateSerializer extends JsonSerializer<Date> {
        public static final DateSerializer INSTANCE = new DateSerializer();

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // 时间戳为 0 的序列化为 null, 这样当数据库表字段设置 1970-01-01 这样的默认值时, 序列化当成 null 处理
            if (U.isNull(value) || value.getTime() <= 0) {
                gen.writeNull();
                return;
            }

            // 标了 JsonFormat 则以注解为主
            Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
            if (U.isNotNull(field)) {
                JsonFormat jsonFormat = getFormatField(field);
                // noinspection DuplicatedCode
                if (U.isNotNull(jsonFormat)) {
                    JsonFormat.Value format = new JsonFormat.Value(jsonFormat);
                    Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                    SimpleDateFormat df = new SimpleDateFormat(format.getPattern(), loc);
                    df.setTimeZone(format.hasTimeZone() ? format.getTimeZone() : provider.getTimeZone());
                    gen.writeString(df.format(value));
                    return;
                }
            }

            // 默认方式
            provider.defaultSerializeDateValue(value, gen);
        }
    }

    /** 序列化 LocalDate, 如果字段上有标 @JsonFormat 则用注解配置, 否则默认显示成 yyyy-MM-dd */
    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        public static final LocalDateSerializer INSTANCE = new LocalDateSerializer();

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // noinspection DuplicatedCode
            if (U.isNull(value)) {
                gen.writeNull();
                return;
            }

            Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
            if (U.isNotNull(field)) {
                JsonFormat jsonFormat = getFormatField(field);
                if (U.isNotNull(jsonFormat)) {
                    JsonFormat.Value format = new JsonFormat.Value(jsonFormat);
                    Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                    gen.writeString(DateTimeFormatter.ofPattern(format.getPattern()).withLocale(loc).format(value));
                    return;
                }
            }

            // 默认显示成 yyyy-MM-dd
            gen.writeString(value.toString());
        }
    }

    /** 序列化 LocalDateTime, 如果字段上有标 @JsonFormat 则用注解配置, 否则默认显示成 yyyy-MM-dd HH:mm:ss */
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        public static final LocalDateTimeSerializer INSTANCE = new LocalDateTimeSerializer();

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // noinspection DuplicatedCode
            if (U.isNull(value)) {
                gen.writeNull();
                return;
            }

            Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
            if (U.isNotNull(field)) {
                JsonFormat jsonFormat = getFormatField(field);
                if (U.isNotNull(jsonFormat)) {
                    JsonFormat.Value format = new JsonFormat.Value(jsonFormat);
                    Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                    gen.writeString(DateTimeFormatter.ofPattern(format.getPattern()).withLocale(loc).format(value));
                    return;
                }
            }

            // 默认显示成 yyyy-MM-dd HH:mm:ss
            gen.writeString(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(value));
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

    /** 反序列化 Date */
    public static class DateDeserializer extends JsonDeserializer<Date> {
        public static final DateDeserializer INSTANCE = new DateDeserializer();

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            Date date = DateUtil.parse(p.getText().trim());
            return (U.isNull(date) || date.getTime() <= 0) ? null : date;
        }
    }

    /** 反序列化 LocalDate */
    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        public static final LocalDateDeserializer INSTANCE = new LocalDateDeserializer();

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return DateUtil.convertLocalDate(DateUtil.parse(p.getText().trim()));
        }
    }

    /** 反序列化 LocalDateTime */
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        public static final LocalDateTimeDeserializer INSTANCE = new LocalDateTimeDeserializer();

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return DateUtil.convertLocalDateTime(DateUtil.parse(p.getText().trim()));
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
