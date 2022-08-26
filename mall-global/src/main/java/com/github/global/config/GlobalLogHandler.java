package com.github.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.json.JsonModule;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class GlobalLogHandler {

    /** 是否进行脱敏 */
    @Value("${json.hasDesensitization:false}")
    private boolean hasDesensitization;

    /** 是否进行数据压缩 */
    @Value("${json.hasCompress:false}")
    private boolean hasCompress;

    /** json 是否进行截断 */
    @Value("${json.cutJson:false}")
    private boolean cutJson;

    /** json 长度大于这个值才进行截断 */
    @Value("${json.cutJsonMax:10000}")
    private int cutJsonMax;

    /** json 截断时只取这个值左右的位数 */
    @Value("${json.cutJsonLeftRightLen:1000}")
    private int cutJsonLeftRightLen;


    private final ObjectMapper objectMapper;
    private final ObjectMapper logDesObjectMapper;

    public GlobalLogHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();

        this.logDesObjectMapper = objectMapper.copy();
        this.logDesObjectMapper.registerModule(JsonModule.LOG_SENSITIVE_MODULE);
    }

    public String toJson(Object data) {
        if (U.isNull(data)) {
            return U.EMPTY;
        }

        String json;
        if (data instanceof String s) {
            json = s;
        } else {
            try {
                json = (hasDesensitization ? logDesObjectMapper : objectMapper).writeValueAsString(data);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("data desensitization exception", e);
                }
                return U.EMPTY;
            }
        }

        String str = hasCompress ? U.compress(json) : json;
        return cutJson ? U.foggyValue(str, cutJsonMax, cutJsonLeftRightLen) : str;
    }
}
