package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在业务接口上
 *
 * @see com.github.common.json.JsonSensitive
 */
public class JsonSensitiveSerializer extends JsonSerializer<Object> {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (U.isNull(obj)) {
            gen.writeNull();
            return;
        }

        if (obj instanceof String) {
            handleString((String) obj, gen);
        }

        else if (obj instanceof Number) {
            handleNumber((Number) obj, gen);
        }

        else if (obj instanceof Date) {
            handleDate((Date) obj, gen, provider);
        }

        else {
            throw new RuntimeException("Annotation @JsonSensitive can not used on types other than String Number Date");
        }
    }

    private void handleString(String value, JsonGenerator gen) throws IOException {
        if (U.isBlank(value)) {
            gen.writeString(U.EMPTY);
            return;
        }

        JsonSensitive sensitive = getAnnotationOnField(gen);
        if (U.isNotNull(sensitive)) {
            int start = Math.max(0, sensitive.start());
            int end = Math.max(0, sensitive.end());
            int length = value.length();

            StringBuilder sbd = new StringBuilder();
            if (start < length) {
                sbd.append(value, 0, start);
            }
            sbd.append(" ***");
            if (end > 0 && length > (start + end + 5)) {
                sbd.append(" ").append(value, length - end, length);
            }
            String text = sbd.toString();
            if (U.isNotBlank(text)) {
                gen.writeString(text.trim());
                return;
            }
        }
        gen.writeString(value);
    }

    private void handleNumber(Number value, JsonGenerator gen) throws IOException {
        JsonSensitive sensitive = getAnnotationOnField(gen);
        double randomNumber = U.callIfNotNull(sensitive, JsonSensitive::randomNumber, 0D);

        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            double d = randomNumber > 0 ? RANDOM.nextDouble(randomNumber) : value.doubleValue();
            int digitsNumber = U.callIfNotNull(sensitive, JsonSensitive::digitsNumber, 0);
            gen.writeString(BigDecimal.valueOf(d).setScale(digitsNumber, RoundingMode.HALF_UP).toString());
            // BigDecimal 或 float 或 double 使用 String 序列化
        }

        else if (value instanceof BigInteger || value instanceof Long) {
            long l = randomNumber > 0 ? RANDOM.nextLong((long) randomNumber) : value.longValue();
            // long 或 BigInt 使用 String 序列化
            gen.writeString(String.valueOf(l));
        }

        else if (value instanceof Integer || value instanceof Short) {
            gen.writeNumber(randomNumber > 0 ? RANDOM.nextInt((int) randomNumber) : value.intValue());
        }

        else {
            gen.writeString(value.toString());
        }
    }

    private void handleDate(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Field field = getAnnotationField(gen);
        JsonSensitive sensitive = U.isNull(field) ? null : field.getAnnotation(JsonSensitive.class);
        long randomNumber = U.callIfNotNull(sensitive, JsonSensitive::randomDate, 0L);
        if (randomNumber > 0) {
            long time = value.getTime();
            long randomTime = RANDOM.nextLong(randomNumber);
            value.setTime((randomTime > time) ? (randomTime - time) : (time - randomTime));
        }

        if (!provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
            JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
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

    private JsonSensitive getAnnotationOnField(JsonGenerator gen) {
        Field field = getAnnotationField(gen);
        return U.isNull(field) ? null : field.getAnnotation(JsonSensitive.class);
    }
    private Field getAnnotationField(JsonGenerator gen) {
        return U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
    }
}
