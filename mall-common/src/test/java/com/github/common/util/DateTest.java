package com.github.common.util;

import com.github.common.date.DateFormatType;
import com.github.common.date.DateUtil;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.util.Locale;

public class DateTest {

    @Test
    public void date() {
        LocalDateTime now = DateUtil.now();
        System.out.println("now     : " + now);
        System.out.println("dt now  : " + DateUtil.formatDateTime(now));
        System.out.println("dtm now : " + DateUtil.formatDateTimeMs(now));

        String date = DateUtil.formatDate(now);
        System.out.println("fd      : " + date);
        System.out.println("ldt     : " + DateUtil.parseToLocalDateTime(date));

        date = DateUtil.format(now, DateFormatType.USA_YYYY_MM_DD_HH_MM_SS);
        System.out.println("usa     : " + date);
        System.out.println("ldt usa : " + DateUtil.parseToLocalDateTime(date));

        System.out.println("-----");

        LocaleContextHolder.setLocale(Locale.CHINA);
        // LocaleContextHolder.setLocale(Locale.US);
        long end = DateUtil.parseToDate("2019-01-03 01:02:02.123").getTime();
        long start = DateUtil.parseToDate("2019-01-01 01:02:03.321").getTime();
        System.out.println(DateUtil.toHumanRoughly(end - start));
        System.out.println(DateUtil.toHuman(end - start));

        System.out.println(DateUtil.formatDateTimeMs(DateUtil.getDayStart(DateUtil.parseToLocalDateTime("2019-01-03 01:02:02"))));
        System.out.println(DateUtil.formatDateTimeMs(DateUtil.getDayEnd(DateUtil.parseToLocalDateTime("2019-01-03 01:02:02"))));
    }
}
