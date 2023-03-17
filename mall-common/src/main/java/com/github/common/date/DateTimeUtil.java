package com.github.common.date;

import com.github.common.util.U;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class DateTimeUtil {

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE_MAP = new ConcurrentHashMap<>();

    private static DateTimeFormatter getFormatter(String type, String timezone) {
        return FORMATTER_CACHE_MAP.computeIfAbsent(type + "-" + U.toStr(timezone), s -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(s);
            if (U.isNotBlank(timezone)) {
                TimeZone timeZone = TimeZone.getTimeZone(timezone);
                if (U.isNotNull(timeZone)) {
                    formatter.withZone(timeZone.toZoneId());
                }
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
    /** 格式化日期 yyyy/MM/dd */
    public static String formatUsaDate(LocalDateTime date) {
        return format(date, DateFormatType.USA_YYYY_MM_DD);
    }
    /** 格式化时间 HH:mm:ss */
    public static String formatTime(LocalDateTime date) {
        return format(date, DateFormatType.HH_MM_SS);
    }
    /** 格式化日期和时间 yyyy-MM-dd HH:mm:ss */
    public static String formatDateTime(LocalDateTime date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 格式化日期 yyyy-MM-dd HH:mm:ss SSS */
    public static String formatDateTimeMs(LocalDateTime date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
    }

    /** 格式化日期对象成字符串 */
    public static String format(TemporalAccessor date, DateFormatType type) {
        return (U.isNull(date) || U.isNull(type)) ? U.EMPTY : format(date, type.getValue());
    }

    public static String format(TemporalAccessor date, String type) {
        return (U.isNull(date) || U.isBlank(type)) ? U.EMPTY : getFormatter(type, null).format(date);
    }

    public static String format(TemporalAccessor date, String type, String timezone) {
        return (U.isNull(date) || U.isBlank(type)) ? U.EMPTY : getFormatter(type, timezone).format(date);
    }

    public static TemporalAccessor parse(String source) {
        if (U.isBlank(source)) {
            return null;
        }

        for (DateFormatType type : DateFormatType.values()) {
            TemporalAccessor accessor = parse(source, type);
            if (U.isNotNull(accessor)) {
                return accessor;
            }
        }
        return null;
    }
    public static TemporalAccessor parse(String source, DateFormatType type) {
        return U.isNotNull(type) ? parse(source, type.getValue()) : null;
    }
    public static TemporalAccessor parse(String source, String type) {
        return U.isBlank(source) || U.isBlank(type) ? null : parse(getFormatter(type, null), source);
    }
    private static TemporalAccessor parse(DateTimeFormatter formatter, String source) {
        // 如果格式是 yyyy-MM-dd, 想要转换成 yyyy-MM-dd HH:mm:ss 这种格式, 会异常, 无法转换, 只能转换成 LocalDate, 因此像下面这样处理
        try {
            LocalDateTime dateTime = formatter.parse(source, LocalDateTime::from);
            if (dateTime != null) {
                return dateTime;
            }
        } catch (Exception ignore) {
        }

        try {
            LocalDate date = formatter.parse(source, LocalDate::from);
            if (date != null) {
                return date;
            }
        } catch (Exception ignore) {
        }

        try {
            LocalTime time = formatter.parse(source, LocalTime::from);
            if (time != null) {
                return time;
            }
        } catch (Exception ignore) {
        }

        try {
            Year year = formatter.parse(source, Year::from);
            if (year != null) {
                return year;
            }
        } catch (Exception ignore) {
        }

        return null;
    }
    public static TemporalAccessor parse(String source, String type, String timezone) {
        return U.isBlank(source) || U.isBlank(type) ? null : parse(getFormatter(type, timezone), source);
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
