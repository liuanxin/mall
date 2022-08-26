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
        else {
            throw new RuntimeException("Annotation @JsonSensitive can not used on types other than String Number Date");
        }
    }

    private void handleString(String value, JsonGenerator gen) throws IOException {
        if (U.isBlank(value)) {
            gen.writeString(value);
        } else {
            JsonSensitive sensitive = getSensitiveAnnotation(gen);
            if (U.isNotNull(sensitive)) {
                gen.writeString(DesensitizationUtil.desString(value, sensitive.start(), sensitive.end()));
            } else {
                gen.writeString(value);
            }
        }
    }

    private void handleNumber(Number value, JsonGenerator gen) throws IOException {
        JsonSensitive sensitive = getSensitiveAnnotation(gen);
        if (U.isNotNull(sensitive)) {
            gen.writeObject(DesensitizationUtil.descNumber(value, sensitive.randomNumber(), sensitive.digitsNumber()));
        } else {
            gen.writeString(value.toString());
        }
    }

    private void handleDate(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        if (U.isNotNull(field)) {
            JsonSensitive sensitive = getSensitiveField(field);
            if (U.isNotNull(sensitive)) {
                long randomDateTimeMillis = sensitive.randomDateTimeMillis();
                if (randomDateTimeMillis != 0) {
                    // 修改时间
                    DesensitizationUtil.descDate(value, randomDateTimeMillis);
                }
            }

            // noinspection DuplicatedCode
            JsonFormat jsonFormat = getFormatField(field);
            if (U.isNotNull(jsonFormat)) {
                // @see com.fasterxml.jackson.databind.ser.std.DateSerializer
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

    private JsonSensitive getSensitiveAnnotation(JsonGenerator gen) {
        Field field = U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
        return U.isNull(field) ? null : getSensitiveField(field);
    }
    private JsonSensitive getSensitiveField(Field field) {
        Class<JsonSensitive> clazz = JsonSensitive.class;
        String key = field.getType().getName() + "#" + field.getName() + "." + clazz.getName();
        JsonSensitive sensitive = (JsonSensitive) FIELD_MAP.get(key);
        if (U.isNull(sensitive)) {
            sensitive = field.getAnnotation(clazz);
            if (U.isNotNull(sensitive)) {
                FIELD_MAP.put(key, sensitive);
            }
        }
        return sensitive;
    }
    private JsonFormat getFormatField(Field field) {
        Class<JsonFormat> clazz = JsonFormat.class;
        String key = field.getType().getName() + "#" + field.getName() + "." + clazz.getName();
        JsonFormat format = (JsonFormat) FIELD_MAP.get(key);
        if (U.isNull(format)) {
            format = field.getAnnotation(clazz);
            if (U.isNotNull(format)) {
                FIELD_MAP.put(key, format);
            }
        }
        return format;
    }
}
