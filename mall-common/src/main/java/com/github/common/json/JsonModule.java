package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.date.DateUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class JsonModule {

    private static final int DES_MAX = 1000;
    private static final int DES_LEN = 200;

    /** 字符串脱敏 */
    public static SimpleModule stringDesensitization() {
        return new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (U.isNull(value)) {
                    gen.writeString(U.EMPTY);
                    return;
                }
                String key = gen.getOutputContext().getCurrentName();
                if (U.isEmpty(key)) {
                    gen.writeString(value);
                    return;
                }

                String data;
                if ("password".equalsIgnoreCase(key)) {
                    data = "***";
                } else {
                    int length = value.length();
                    data = (length <= DES_MAX) ? value : (value.substring(0, DES_LEN) + " *** " + value.substring(length - DES_LEN));
                }
                gen.writeString(data);
            }
        });
    }

    /** 序列化 BigDecimal 小数位不足 2 位的返回 2 位 */
    public static SimpleModule bigDecimalSerializer() {
        return new SimpleModule().addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (U.isNull(value)) {
                    gen.writeString(U.EMPTY);
                    return;
                }

                // 不足 2 位则输出 2 位小数
                if (value.scale() < 2) {
                    gen.writeString(value.setScale(2, RoundingMode.UNNECESSARY).toString());
                } else {
                    gen.writeString(value.toString());
                }
            }
        });
    }


    // ======================================== 上面是序列化, 下面是反序列化 ========================================


    /** 反序列化 Date, 序列化使用全局配置, 或者属性上的 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 注解 */
    public static SimpleModule dateDeserializer() {
        return new SimpleModule().addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return DateUtil.parse(p.getText().trim());
            }
        });
    }
}
