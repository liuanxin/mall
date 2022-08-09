package com.github.common.util;

import com.github.common.date.DateFormatType;
import com.github.common.date.DateUtil;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class DateTest {

    @Test
    public void date() {
        Date now = DateUtil.now();
        System.out.println(now);
        System.out.println(DateUtil.formatDateTime(now));
        System.out.println(DateUtil.formatDateTimeMs(now));

        String date = DateUtil.formatDate(now);
        System.out.println(date);
        System.out.println(DateUtil.parse(date));

        date = DateUtil.format(now, DateFormatType.USA_YYYY_MM_DD_HH_MM_SS);
        System.out.println(DateUtil.parse(date));

        LocaleContextHolder.setLocale(Locale.CHINA);
        // LocaleContextHolder.setLocale(Locale.US);
        long end = Objects.requireNonNull(DateUtil.parse("2019-01-03 01:02:02.123")).getTime();
        long start = Objects.requireNonNull(DateUtil.parse("2019-01-01 01:02:03.321")).getTime();
        System.out.println(DateUtil.toHumanRoughly(end - start));
        System.out.println(DateUtil.toHuman(end - start));

        System.out.println(DateUtil.getDayStart(DateUtil.parse("2019-01-03 01:02:02")));
    }
}
