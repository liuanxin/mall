package com.github.common.date;

import com.github.common.util.U;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DateTimeUtil {

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String type) {
        return getFormatter(type, null, null);
    }
    private static DateTimeFormatter getFormatter(String type, String timezone) {
        return getFormatter(type, timezone, null);
    }
    private static DateTimeFormatter getFormatter(String type, Locale locale) {
        return getFormatter(type, null, locale);
    }
    private static DateTimeFormatter getFormatter(String type, String timezone, Locale locale) {
        List<String> keyList = new ArrayList<>();
        keyList.add(type);
        boolean hasTimeZone = U.isNotBlank(timezone);
        if (hasTimeZone) {
            keyList.add(timezone);
        }
        boolean hasLocale = U.isNotNull(locale);
        if (hasLocale) {
            keyList.add(U.toStr(locale));
        }
        return FORMATTER_CACHE.computeIfAbsent(String.join("-", keyList), s -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(s);
            if (hasTimeZone) {
                TimeZone timeZone = TimeZone.getTimeZone(timezone);
                if (U.isNotNull(timeZone)) {
                    formatter.withZone(timeZone.toZoneId());
                }
            }
            if (hasLocale) {
                formatter.withLocale(locale);
            }
            return formatter;
        });
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /** 返回 yyyy-MM-dd HH:mm:ss 格式的当前时间 */
    public static String nowDateTime() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 返回 yyyy-MM-dd HH:mm:ss SSS 格式的当前时间 */
    public static String nowDateTimeMs() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SS_SSS);
    }
    /** 获取当前时间日期的字符串 */
    public static String now(DateFormatType dateFormatType) {
        return format(now(), dateFormatType);
    }
    /** 格式化日期 yyyy-MM-dd */
    public static String formatDate(LocalDateTime date) {
        return format(date, DateFormatType.YYYY_MM_DD);
    }
    /** 格式化时间 HH:mm:ss */
    public static String formatTime(LocalDateTime date) {
        return format(date, DateFormatType.HH_MM_SS);
    }
    /** 格式化日期和时间 yyyy-MM-dd HH:mm:ss */
    public static String formatDateTime(LocalDateTime date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 格式化日期 yyyy-MM-dd HH:mm:ss.SSS */
    public static String formatDateTimeMs(LocalDateTime date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
    }

    /**
     * 默认格式化
     *   日期时间: yyyy-MM-dd HH:mm:ss
     *   日期:    yyyy-MM-dd
     *   时间:    HH:mm:ss
     *   年:     yyyy
     */
    public static String format(TemporalAccessor date) {
        if (date instanceof LocalDateTime) {
            return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS.getValue());
        } else if (date instanceof LocalDate) {
            return format(date, DateFormatType.YYYY_MM_DD.getValue());
        } else if (date instanceof LocalTime) {
            return format(date, DateFormatType.HH_MM_SS.getValue());
        } else if (date instanceof Year) {
            return format(date, DateFormatType.YYYY.getValue());
        } else {
            return date.toString();
        }
    }

    /** 格式化日期对象成字符串 */
    public static String format(TemporalAccessor date, DateFormatType type) {
        return (U.isNull(date) || U.isNull(type)) ? U.EMPTY : format(date, type.getValue());
    }

    public static String format(TemporalAccessor date, String type) {
        return format(date, type, Locale.getDefault());
    }

    public static String format(TemporalAccessor date, String type, String timezone) {
        return (U.isNull(date) || U.isBlank(type)) ? U.EMPTY : getFormatter(type, timezone).format(date);
    }

    public static String format(TemporalAccessor date, String type, Locale locale) {
        return (U.isNull(date) || U.isBlank(type)) ? U.EMPTY : getFormatter(type, locale).format(date);
    }

    public static TemporalAccessor parse(String source) {
        if (U.isBlank(source)) {
            return null;
        }

        for (DateFormatType type : DateFormatType.values()) {
            if (type.isLocalDateTimeType()) {
                try {
                    LocalDateTime localDateTime = parseLocalDateTime(source, type.getValue());
                    if (U.isNotNull(localDateTime)) {
                        return localDateTime;
                    }
                } catch (Exception ignore) {
                }
            }
            if (type.isLocalDateType()) {
                LocalDate localDate = parseLocalDate(source, type.getValue());
                if (U.isNotNull(localDate)) {
                    return localDate;
                }
            }
            if (type.isLocalTimeType()) {
                LocalTime localTime = parseLocalTime(source, type.getValue());
                if (U.isNotNull(localTime)) {
                    return localTime;
                }
            }
            if (type.isYearType()) {
                Year year = parseYear(source, type.getValue());
                if (U.isNotNull(year)) {
                    return year;
                }
            }
        }
        return null;
    }
    public static TemporalAccessor parse(String source, DateFormatType type) {
        return U.isNotNull(type) ? parse(source, type.getValue()) : null;
    }
    public static TemporalAccessor parse(String source, String type) {
        return U.isBlank(source) || U.isBlank(type) ? null : parse(getFormatter(type), source);
    }
    public static TemporalAccessor parse(String source, String type, String timezone) {
        return U.isBlank(source) || U.isBlank(type) ? null : parse(getFormatter(type, timezone), source);
    }
    private static TemporalAccessor parse(DateTimeFormatter formatter, String source) {
        // 如果格式是 yyyy-MM-dd, 数据是 2022-01-01, parse 成 LocalDateTime 会抛异常
        try {
            LocalDateTime localDateTime = formatter.parse(source, LocalDateTime::from);
            if (U.isNotNull(localDateTime)) {
                return localDateTime;
            }
        } catch (Exception ignore) {
        }

        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalDate 不会异常
        try {
            LocalDate localDate = formatter.parse(source, LocalDate::from);
            if (U.isNotNull(localDate)) {
                return localDate;
            }
        } catch (Exception ignore) {
        }

        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalTime 不会异常
        try {
            LocalTime time = formatter.parse(source, LocalTime::from);
            if (U.isNotNull(time)) {
                return time;
            }
        } catch (Exception ignore) {
        }

        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 Year 不会异常
        try {
            Year year = formatter.parse(source, Year::from);
            if (U.isNotNull(year)) {
                return year;
            }
        } catch (Exception ignore) {
        }

        return null;
    }
    public static LocalDateTime parseLocalDateTime(String source, String type) {
        // 如果格式是 yyyy-MM-dd, 数据是 2022-01-01, parse 成 LocalDateTime 会抛异常
        return getFormatter(type).parse(source, LocalDateTime::from);
    }
    public static LocalDate parseLocalDate(String source, String type) {
        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalDate 不会异常
        return getFormatter(type).parse(source, LocalDate::from);
    }
    public static LocalTime parseLocalTime(String source, String type) {
        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalTime 不会异常
        return getFormatter(type).parse(source, LocalTime::from);
    }
    public static Year parseYear(String source, String type) {
        // 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 Year 不会异常
        return getFormatter(type).parse(source, Year::from);
    }

    /** 获取一个日期所在天的最开始的时间(00:00:00 000), 对日期查询尤其有用 */
    public static LocalDateTime getDayStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(LocalTime.MIN);
    }
    /** 获取一个日期所在天的最晚的时间(23:59:59 999), 对日期查询尤其有用 */
    private static LocalDateTime getDayEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(LocalTime.MAX);
    }

    /** 获取一个日期所在星期天(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static LocalDateTime getSundayStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(DayOfWeek.SUNDAY).with(LocalTime.MIN);
    }
    /** 获取一个日期所在星期六(星期六是最后一天)的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getSaturdayEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(DayOfWeek.SATURDAY).with(LocalTime.MAX);
    }

    /** 获取一个日期所在星期一(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static LocalDateTime getMondayStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
    }
    /** 获取一个日期所在星期天(星期天是最后一天)的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getSundayEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(DayOfWeek.SUNDAY).with(LocalTime.MAX);
    }

    /** 获取一个日期所在月的第一毫秒(00:00:00 000) */
    public static LocalDateTime getMonthStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.withDayOfMonth(1).with(LocalTime.MIN);
    }
    /** 获取一个日期所在月的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getMonthEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
    }

    /** 获取一个日期所在季度的第一毫秒(00:00:00 000) */
    public static LocalDateTime getQuarterStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.withMonth(date.get(IsoFields.QUARTER_OF_YEAR) * 3 - 2).withDayOfMonth(1).with(LocalTime.MIN);
    }
    /** 获取一个日期所在季度的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getQuarterEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.withMonth(date.get(IsoFields.QUARTER_OF_YEAR) * 3).with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
    }

    /** 获取一个日期所在年的第一毫秒(23:59:59 999) */
    public static LocalDateTime getYearStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.withMonth(1).withDayOfMonth(1).with(LocalTime.MIN);
    }
    /** 获取一个日期所在年的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getYearEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.withMonth(12).withDayOfMonth(31).with(LocalTime.MAX);
    }

    /**
     * 取得指定日期 N 天后的日期
     *
     * @param year 正数表示多少年后, 负数表示多少年前
     */
    public static LocalDateTime addYears(LocalDateTime date, int year) {
        return U.isNull(date) ? null : date.plusYears(year);
    }
    /**
     * 取得指定日期 N 个月后的日期
     *
     * @param month 正数表示多少月后, 负数表示多少月前
     */
    public static LocalDateTime addMonths(LocalDateTime date, int month) {
        return U.isNull(date) ? null : date.plusMonths(month);
    }
    /**
     * 取得指定日期 N 天后的日期
     *
     * @param day 正数表示多少天后, 负数表示多少天前
     */
    public static LocalDateTime addDays(LocalDateTime date, int day) {
        return U.isNull(date) ? null : date.plusDays(day);
    }
    /**
     * 取得指定日期 N 周后的日期
     *
     * @param week 正数表示多少周后, 负数表示多少周前
     */
    public static LocalDateTime addWeeks(LocalDateTime date, int week) {
        return U.isNull(date) ? null : date.plusWeeks(week);
    }
    /**
     * 取得指定日期 N 小时后的日期
     *
     * @param hour 正数表示多少小时后, 负数表示多少小时前
     */
    public static LocalDateTime addHours(LocalDateTime date, int hour) {
        return U.isNull(date) ? null : date.plusHours(hour);
    }
    /**
     * 取得指定日期 N 分钟后的日期
     *
     * @param minute 正数表示多少分钟后, 负数表示多少分钟前
     */
    public static LocalDateTime addMinute(LocalDateTime date, int minute) {
        return U.isNull(date) ? null : date.plusMinutes(minute);
    }
    /**
     * 取得指定日期 N 秒后的日期
     *
     * @param second 正数表示多少秒后, 负数表示多少秒前
     */
    public static LocalDateTime addSeconds(LocalDateTime date, int second) {
        return U.isNull(date) ? null : date.plusSeconds(second);
    }

    /** 传入的时间是不是当月当日. 用来验证生日 */
    public static boolean wasBirthday(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();
        return now.getMonth() == date.getMonth() && now.getDayOfMonth() == date.getDayOfMonth();
    }

    /** 计算两个日期之间相差的年数. 如果 start 比 end 大将会返回负数 */
    public static long betweenYear(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.YEARS.between(start, end);
    }

    /** 计算两个日期之间相差的月数. 如果 start 比 end 大将会返回负数 */
    public static long betweenMonth(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(start, end);
    }

    /** 计算两个日期之间相差的星期数. 如果 start 比 end 大将会返回负数 */
    public static long betweenWeek(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.WEEKS.between(start, end);
    }

    /** 计算两个日期之间相差的天数. 如果 start 比 end 大将会返回负数 */
    public static long betweenDay(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /** 计算两个日期之间相差的小时数. 如果 start 比 end 大将会返回负数 */
    public static long betweenHour(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /** 计算两个日期之间相差的小时数. 如果 start 比 end 大将会返回负数 */
    public static long betweenMinute(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /** 计算两个日期之间相差的秒数. 如果 start 比 end 大将会返回负数 */
    public static long betweenSecond(LocalDateTime start, LocalDateTime end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }
}
