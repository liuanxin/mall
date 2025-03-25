package com.github.global.converter;

import com.github.common.date.Dates;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;

/**
 * 字符串转换为日期对象. 匹配多种形式!<br>
 * 前台传过来的不管是 yyyy-MM-dd HH:mm:ss 还是 yyyy-MM-dd 都能转换成 LocalDate 对象.<br>
 * 全部的格式参见 DateFormatType, 当没有匹配时返回 null 而不是抛出异常.<br><br>
 *
 * 此 convert 用于替代 org.springframework.format.annotation.DateTimeFormat 注解
 *
 * @see com.github.common.date.DateFormatType
 * @see org.springframework.format.annotation.DateTimeFormat
 */
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(String source) {
        return Dates.parseToLocalDate(source.trim());
    }
}
