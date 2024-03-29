package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在业务接口上
 *
 * @see com.github.common.json.JsonSensitive
 */
public class JsonSensitiveSerializer extends JsonSerializer<Object> {

    private static final Map<String, ? super Annotation> FIELD_MAP = new ConcurrentHashMap<>();

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (U.isNull(obj)) {
            gen.writeNull();
        }
        else if (obj instanceof String s) {
            handleString(s, gen);
        }
        else if (obj instanceof Number n) {
            handleNumber(n, gen);
        }
        else if (obj instanceof Date d) {
            handleDate(d, gen, provider);
        }
        else if (obj instanceof LocalDate d) {
            Date date = Date.from(LocalDateTime.of(d, LocalTime.MIN).atZone(ZoneId.systemDefault()).toInstant());
            handleDate(date, gen, provider);
        }
        else if (obj instanceof LocalDateTime d) {
            Date date = Date.from(d.atZone(ZoneId.systemDefault()).toInstant());
            handleDate(date, gen, provider);
        }
        else {
            throw new RuntimeException("Annotation @JsonSensitive can not used on types other than String Number Date");
        }
    }

    private void handleString(String value, JsonGenerator gen) throws IOException {
        if (U.isNotBlank(value)) {
            Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
            if (U.isNotNull(field)) {
                JsonSensitiveString sensitiveField = getFieldAnnotation(field, JsonSensitiveString.class);
                if (U.isNotNull(sensitiveField)) {
                    gen.writeString(sensitiveField.value().des(value, sensitiveField.showLen()));
                    return;
                }

                JsonSensitive sensitive = getFieldAnnotation(field, JsonSensitive.class);
                if (U.isNotNull(sensitive)) {
                    gen.writeString(DesensitizationUtil.desString(value, sensitive.start(), sensitive.end()));
                    return;
                }
            }
        }
        gen.writeString(value);
    }

    private void handleNumber(Number value, JsonGenerator gen) throws IOException {
        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        if (U.isNotNull(field)) {
            JsonSensitive sensitive = getFieldAnnotation(field, JsonSensitive.class);
            if (U.isNotNull(sensitive)) {
                gen.writeObject(DesensitizationUtil.desNumber(value, sensitive.randomNumber(), sensitive.digitsNumber()));
            }
        }
        gen.writeString(value.toString());
    }

    /** @see com.fasterxml.jackson.databind.ser.std.DateSerializer */
    private void handleDate(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        if (U.isNotNull(field)) {
            JsonSensitive sensitive = getFieldAnnotation(field, JsonSensitive.class);
            if (U.isNotNull(sensitive)) {
                long randomDateTimeMillis = sensitive.randomDateTimeMillis();
                if (randomDateTimeMillis != 0) {
                    // 修改时间
                    value.setTime(DesensitizationUtil.desDate(value, randomDateTimeMillis));
                }
            }

            JsonFormat jsonFormat = getFieldAnnotation(field, JsonFormat.class);
            if (U.isNotNull(jsonFormat)) {
                JsonFormat.Value format = new JsonFormat.Value(jsonFormat);
                Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                SimpleDateFormat df = new SimpleDateFormat(format.getPattern(), loc);
                df.setTimeZone(format.hasTimeZone() ? format.getTimeZone() : provider.getTimeZone());
                gen.writeString(df.format(value));
                return;
            }
        }
        provider.defaultSerializeDateValue(value, gen);
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T getFieldAnnotation(Field field, Class<T> clazz) {
        String key = field.getType().getName() + "#" + field.getName() + "." + clazz.getName();
        T sen = (T) FIELD_MAP.get(key);
        if (U.isNotNull(sen)) {
            return sen;
        }

        T s = field.getAnnotation(clazz);
        if (U.isNotNull(s)) {
            FIELD_MAP.put(key, s);
        }
        return s;
    }
}
