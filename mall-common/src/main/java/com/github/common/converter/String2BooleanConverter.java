package com.github.common.converter;

import com.github.common.util.U;
import org.springframework.core.convert.converter.Converter;

/**
 * 字符串转换为 Boolean 类型, 传 true 1 on yes 才是 true, 否则是 false
 * spring 自带的实现中, 只有几种才是 false, 不传将是 null, 转换出错会抛异常.
 *
 * @see org.springframework.core.convert.support.StringToBooleanConverter
 */
@SuppressWarnings({"JavadocReference", "NullableProblems"})
public class String2BooleanConverter implements Converter<String, Boolean> {

    @Override
    public Boolean convert(String source) {
        return U.getBoolean(source);
    }
}
