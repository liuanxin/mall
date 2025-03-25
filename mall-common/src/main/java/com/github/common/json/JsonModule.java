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
import com.github.common.date.Dates;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonModule {

    /** 全局序列化反序列化模块 */
    public static final SimpleModule GLOBAL_MODULE = new SimpleModule()
            // .addSerializer(Integer.class, ToStringSerializer.instance)
            // .addSerializer(int.class, ToStringSerializer.instance)
            // long float double 序列化时用 String 类型, 避免前端处理时精度等一些奇怪的问题
            .addSerializer(Long.class, ToStringSerializer.instance)
            .addSerializer(long.class, ToStringSerializer.instance)
            .addSerializer(Float.class, ToStringSerializer.instance)
            .addSerializer(float.class, ToStringSerializer.instance)
            .addSerializer(Double.class, ToStringSerializer.instance)
            .addSerializer(double.class, ToStringSerializer.instance)
            .addSerializer(BigInteger.class, ToStringSerializer.instance)

            .addSerializer(BigDecimal.class, BigDecimalSerializer.INSTANCE)
            .addSerializer(Date.class, DateSerializer.INSTANCE) // com.fasterxml.jackson.databind.ser.std.DateSerializer
            .addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE)
            .addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE)

            .addDeserializer(boolean.class, BoolDeserializer.INSTANCE)
            .addDeserializer(Boolean.class, BooleanDeserializer.INSTANCE)
            .addDeserializer(BigDecimal.class, BigDecimalDeserializer.INSTANCE)
            .addDeserializer(Date.class, DateDeserializer.INSTANCE)
            .addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE)
            .addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE);


    /** 日志脱敏用到的序列化模块 */
    public static final SimpleModule LOG_SENSITIVE_MODULE = new SimpleModule()
            .addSerializer(String.class, GlobalLogSensitiveSerializer.INSTANCE);


    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, JsonFormat.Value> FIELD_FORMAT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, DateTimeFormatter> FORMAT_CACHE = new ConcurrentHashMap<>();
    private static JsonFormat.Value getJsonFormatOnField(JsonGenerator gen) {
        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        if (U.isNull(field)) {
            return null;
        } else {
            String key = field.getType().getName() + "#" + field.getName();
            return FIELD_FORMAT_CACHE.computeIfAbsent(key, s -> {
                JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
                return U.isNotNull(jsonFormat) ? new JsonFormat.Value(jsonFormat) : null;
            });
        }
    }
    private static String format(TemporalAccessor value, JsonGenerator gen, SerializerProvider provider) {
        JsonFormat.Value format = getJsonFormatOnField(gen);
        if (U.isNull(format)) {
            return null;
        } else {
            DateTimeFormatter formatter = FORMAT_CACHE.computeIfAbsent(format.getPattern(), s -> {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(s);
                Locale locale = format.hasLocale() ? format.getLocale() : provider.getLocale();
                if (U.isNotNull(locale)) {
                    dateTimeFormatter.withLocale(locale);
                }
                return dateTimeFormatter;
            });
            return formatter.format(value);
        }
    }


    /** 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在日志打印上 */
    public static class GlobalLogSensitiveSerializer extends JsonSerializer<String> {
        public static final GlobalLogSensitiveSerializer INSTANCE = new GlobalLogSensitiveSerializer();

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(DesensitizationUtil.desWithKey(gen.getOutputContext().getCurrentName(), value));
        }
    }

    /** 序列化 Date: 将 1970-01-01 当成 null, 如果字段上有标 @JsonFormat 则用标的为主, 否则以默认方式处理 */
    public static class DateSerializer extends JsonSerializer<Date> {
        public static final DateSerializer INSTANCE = new DateSerializer();

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // 时间戳为 0 的是否序列化为 null, 这样当数据库表字段设置 1970-01-01 这样的默认值时, 序列化时就是 null 值
            if (U.isNull(value)/* || value.getTime() <= 0*/) {
                gen.writeNull();
            } else {
                // 标了 JsonFormat 则以注解为主
                JsonFormat.Value format = getJsonFormatOnField(gen);
                if (U.isNotNull(format)) {
                    Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                    SimpleDateFormat df = new SimpleDateFormat(format.getPattern(), loc);
                    df.setTimeZone(format.hasTimeZone() ? format.getTimeZone() : provider.getTimeZone());
                    gen.writeString(df.format(value));
                } else {
                    // 默认方式: 当 SerializationFeature.WRITE_DATES_AS_TIMESTAMPS 为 true 时序列化成时间戳, 为 false 则以定义的 DateFormat 进行序列化操作
                    provider.defaultSerializeDateValue(value, gen);
                }
            }
        }
    }

    /** 序列化 LocalDate, 如果字段上有标 @JsonFormat 则用注解配置, 否则默认显示成 yyyy-MM-dd */
    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        public static final LocalDateSerializer INSTANCE = new LocalDateSerializer();

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (U.isNull(value)) {
                gen.writeNull();
            } else {
                // 标了 JsonFormat 则以注解为主
                String dateFormat = format(value, gen, provider);
                if (U.isNotNull(dateFormat)) {
                    gen.writeString(dateFormat);
                } else {
                    // 默认显示成 yyyy-MM-dd
                    gen.writeString(DATE_FORMAT.format(value));
                }
            }
        }
    }

    /** 序列化 LocalDateTime, 如果字段上有标 @JsonFormat 则用注解配置, 否则默认显示成 yyyy-MM-dd HH:mm:ss */
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        public static final LocalDateTimeSerializer INSTANCE = new LocalDateTimeSerializer();

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (U.isNull(value)) {
                gen.writeNull();
            } else {
                // 标了 JsonFormat 则以注解为主
                String dateFormat = format(value, gen, provider);
                if (U.isNotNull(dateFormat)) {
                    gen.writeString(dateFormat);
                } else {
                    // 默认显示成 yyyy-MM-dd HH:mm:ss
                    gen.writeString(DATE_TIME_FORMAT.format(value));
                }
            }
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
            Date date = Dates.parseToDate(p.getText().trim());
            // 时间戳为 0 的是否反序列化为 null, 这样当数据库表字段设置 1970-01-01 这样的默认值时, 反序列化时就是 null 值
            return (U.isNull(date)/* || date.getTime() <= 0*/) ? null : date;
        }
    }

    /** 反序列化 LocalDate */
    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        public static final LocalDateDeserializer INSTANCE = new LocalDateDeserializer();

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return Dates.parseToLocalDate(p.getText().trim());
        }
    }

    /** 反序列化 LocalDateTime */
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        public static final LocalDateTimeDeserializer INSTANCE = new LocalDateTimeDeserializer();

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return Dates.parseToLocalDateTime(p.getText().trim());
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

    /** 反序列化 boolean(true 或 false) */
    public static class BoolDeserializer extends JsonDeserializer<Boolean> {
        public static final BoolDeserializer INSTANCE = new BoolDeserializer();

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return U.toBool(p.getText());
        }
    }
    /** 反序列化 Boolean(null 或 true 或 false) */
    public static class BooleanDeserializer extends JsonDeserializer<Boolean> {
        public static final BooleanDeserializer INSTANCE = new BooleanDeserializer();

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return U.toBoolean(p.getText());
        }
    }
}
