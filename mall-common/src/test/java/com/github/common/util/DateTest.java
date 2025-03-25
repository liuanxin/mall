package com.github.common.util;

import com.github.common.date.Dates;
import com.github.common.date.FormatType;
import org.junit.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.util.Locale;

public class DateTest {

    @Test
    public void date() {
        LocalDateTime now = Dates.now();
        System.out.println("now     : " + now);
        System.out.println("dt now  : " + Dates.formatDateTime(now));
        System.out.println("dtm now : " + Dates.formatDateTimeMs(now));

        String date = Dates.formatDate(now);
        System.out.println("fd      : " + date);
        System.out.println("ldt     : " + Dates.parseToLocalDateTime(date));

        date = Dates.format(now, FormatType.USA_YYYY_MM_DD_HH_MM_SS);
        System.out.println("usa     : " + date);
        System.out.println("ldt usa : " + Dates.parseToLocalDateTime(date));

        System.out.println("-----");

        LocaleContextHolder.setLocale(Locale.CHINA);
        // LocaleContextHolder.setLocale(Locale.US);
        long end = Dates.parseToDate("2019-01-03 01:02:02.123").getTime();
        long start = Dates.parseToDate("2019-01-01 01:02:03.321").getTime();
        System.out.println(Dates.toHumanRoughly(end - start));
        System.out.println(Dates.toHuman(end - start));

        System.out.println(Dates.formatDateTimeMs(Dates.getDayStart(Dates.parseToLocalDateTime("2019-01-03 01:02:02"))));
        System.out.println(Dates.formatDateTimeMs(Dates.getDayEnd(Dates.parseToLocalDateTime("2019-01-03 01:02:02"))));
    }
}
