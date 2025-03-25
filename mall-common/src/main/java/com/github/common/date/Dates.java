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

public class Dates {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = 365 * DAY;

    private static final Map<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap<>();

    /** 带时区 2020-01-02T03:04:05+08:00 或 2020-01-02T03:04:05.678+08:00 格式的字符串换成 date */
    public static Date isoToDate(String str) {
        if (U.isBlank(str)) {
            return null;
        }
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return U.isNull(offsetDateTime) ? null : Date.from(offsetDateTime.toInstant());
    }

    /** Date 转换成带时区 2020-01-02T03:04:05+08:00 格式的字符串(使用系统时区, 中国是 +08:00 时区) */
    public static String dateToIso(Date date) {
        if (U.isNull(date)) {
            return U.EMPTY;
        }
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static LocalDateTime toLocalDateTime(TemporalAccessor date) {
        if (U.isNull(date)) {
            return null;
        }
        if (date instanceof LocalDate) {
            return LocalDateTime.of((LocalDate) date, LocalTime.MIN);
        }
        if (date instanceof LocalTime) {
            return LocalDateTime.of(LocalDate.MIN, (LocalTime) date);
        }
        return LocalDateTime.from(date);
    }
    public static LocalDateTime toLocalDateTime(Date date) {
        return U.isNull(date) ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
    public static LocalDateTime toLocalDateTime(LocalDate date) {
        return U.isNull(date) ? null : LocalDateTime.of(date, LocalTime.MIN);
    }

    public static LocalDate toLocalDate(TemporalAccessor date) {
        return U.isNull(date) ? null : LocalDate.from(date);
    }
    public static LocalDate toLocalDate(Date date) {
        LocalDateTime localDateTime = toLocalDateTime(date);
        return U.isNull(localDateTime) ? null : localDateTime.toLocalDate();
    }
    public static LocalDate toLocalDate(LocalDateTime date) {
        return U.isNull(date) ? null : date.toLocalDate();
    }

    public static Date toDate(TemporalAccessor date) {
        return U.isNull(date) ? null : toDate(toLocalDateTime(date));
    }
    public static Date toDate(LocalDateTime dateTime) {
        return U.isNull(dateTime) ? null : Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    public static Date toDate(LocalDate date) {
        return U.isNull(date) ? null : toDate(toLocalDateTime(date));
    }

    /** 到秒的时间戳(如 MySQL 的 UNIX_TIMESTAMP() 函数) */
    public static Date toDate(int timestamp) {
        return new Date(timestamp * 1000L);
    }

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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(type);
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
    /** 格式化日期 yyyy-MM-dd */
    public static String formatDate(LocalDate date) {
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
    /** 格式化日期 yyyy/MM/dd */
    public static String formatUsaDate(LocalDate date) {
        return format(toLocalDate(date), DateFormatType.USA_YYYY_MM_DD);
    }
    /** 格式化日期 yyyy-MM-dd HH:mm:ss.SSS */
    public static String formatDateTimeMs(Date date) {
        return format(toLocalDateTime(date), DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
    }

    /**
     * <pre>
     * 默认格式化
     *   日期时间: yyyy-MM-dd HH:mm:ss
     *   日期:    yyyy-MM-dd
     *   时间:    HH:mm:ss
     *   年:     yyyy
     * </pre>
     */
    public static String format(TemporalAccessor date) {
        if (date instanceof LocalDateTime) {
            return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS.getValue());
        } else if (date instanceof LocalDate) {
            return format(date, DateFormatType.YYYY_MM_DD.getValue());
        } else if (date instanceof LocalTime) {
            return format(date, DateFormatType.HH_MM_SS.getValue());
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

    private static TemporalAccessor parse(String source) {
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
                try {
                    LocalDate localDate = parseLocalDate(source, type.getValue());
                    if (U.isNotNull(localDate)) {
                        return localDate;
                    }
                } catch (Exception ignore) {
                }
            }
            if (type.isLocalTimeType()) {
                try {
                    LocalTime localTime = parseLocalTime(source, type.getValue());
                    if (U.isNotNull(localTime)) {
                        return localTime;
                    }
                } catch (Exception ignore) {
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

        return null;
    }
    /** 如果格式是 yyyy-MM-dd, 数据是 2022-01-01, parse 成 LocalDateTime 会抛异常 */
    public static LocalDateTime parseLocalDateTime(String source, String type) {
        return getFormatter(type).parse(source, LocalDateTime::from);
    }
    /** 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalDate 不会抛异常 */
    public static LocalDate parseLocalDate(String source, String type) {
        return getFormatter(type).parse(source, LocalDate::from);
    }
    /** 如果格式是 yyyy-MM-dd HH:mm:ss, 数据是 2022-01-01 01:02:03, parse 成 LocalTime 不会抛异常 */
    public static LocalTime parseLocalTime(String source, String type) {
        return getFormatter(type).parse(source, LocalTime::from);
    }

    public static LocalDate parseToLocalDate(String source) {
        return toLocalDate(parse(source));
    }
    public static LocalDateTime parseToLocalDateTime(String source) {
        return toLocalDateTime(parse(source));
    }
    public static Date parseToDate(String source) {
        return toDate(parse(source.trim()));
    }

    /**
     * <pre>
     * 一分钟内就返回 刚刚, 一小时内就返回 x 分钟, 一天内就返回 xx 小时, 一年内就返回 xxx 天, 否则返回 xxxx 年, 如
     *
     * toHumanRoughly("2019-01-01 01:03:02".ms - "2019-01-01 01:02:03".ms) ==> 刚刚
     * toHumanRoughly("2019-01-02 01:02:03".ms - "2019-01-01 01:02:03".ms) ==> 昨天
     * toHumanRoughly("2019-01-03 01:02:03".ms - "2019-01-01 01:02:03".ms) ==> 前天
     *
     * toHumanRoughly("2019-01-01 01:03:03".ms - "2019-01-01 01:02:03".ms) ==> 1 分钟前
     * toHumanRoughly("2019-01-01 02:02:02".ms - "2019-01-01 01:02:03".ms) ==> 59 分钟前
     *
     * toHumanRoughly("2019-01-01 02:02:03".ms - "2019-01-01 01:02:03".ms) ==> 1 小时前
     * toHumanRoughly("2019-01-02 01:02:02".ms - "2019-01-01 01:02:03".ms) ==> 23 小时前
     *
     * toHumanRoughly("2019-01-02 01:02:03".ms - "2019-01-01 01:02:03".ms) ==> 1 天前
     * toHumanRoughly("2019-01-11".ms - "2019-01-01".ms) ==> 10 天前
     * toHumanRoughly("2020-01-01 01:02:02".ms - "2019-01-01 01:02:03".ms) ==> 364 天前
     *
     * toHumanRoughly("2020-01-01 01:02:03".ms - "2019-01-01 01:02:03".ms) ==> 1 年前
     * toHumanRoughly("2020-01-01".ms - "2010-01-01".ms) ==> 10 年前
     *
     * @param intervalMs 间隔毫秒数, (大的时间戳 - 小的时间戳)会返回 x 分钟前, 反之会返回 x 分钟后
     * </pre>
     */
    public static String toHumanRoughly(long intervalMs) {
        boolean hasCn = Locale.getDefault() == Locale.CHINA;
        if (intervalMs == 0) {
            return hasCn ? "刚刚" : "Now";
        }

        boolean flag = (intervalMs < 0);
        long ms = (flag ? -intervalMs : intervalMs);
        if (ms < MINUTE) {
            return hasCn ? "刚刚" : "Now";
        }

        String state = flag ? (hasCn ? "后" : " later") : (hasCn ? "前" : " ago");

        long minute = ms / MINUTE;
        if (minute < 60) {
            return minute + (hasCn ? " 分钟" : (minute > 1 ? " minutes" : " minute")) + state;
        }

        long hour = minute / 60;
        if (hour < 24) {
            return hour + (hasCn ? " 小时" : (hour > 1 ? " hours" : " hour")) + state;
        }

        long day = hour / 24;
        if (day == 1) {
            return flag ? (hasCn ? "明天" : "tomorrow") : (hasCn ? "昨天" : "yesterday");
        } else if (day == 2) {
            return flag ? (hasCn ? "后天" : "after tomorrow") : (hasCn ? "前天" : "before yesterday");
        } else if (day < 365) {
            return day + (hasCn ? " 天" : " days") + state;
        } else {
            return (day / 365) + (hasCn ? " 年" : ((day > (365 * 2)) ? " years" : " year")) + state;
        }
    }
    /** 如: toHuman(36212711413L) ==> 1 年 54 天 3 小时 5 分 11 秒 413 毫秒 */
    public static String toHuman(long intervalMs) {
        if (intervalMs == 0) {
            return "0";
        }
        boolean hasCn = Locale.getDefault().equals(Locale.CHINA);

        boolean flag = (intervalMs < 0);
        long ms = Math.abs(intervalMs);

        long year = ms / YEAR;
        long y = ms % YEAR;

        long day = y / DAY;
        long d = y % DAY;

        long hour = d / HOUR;
        long h = d % HOUR;

        long minute = h / MINUTE;
        long mi = h % MINUTE;

        long second = mi / SECOND;
        long m = mi % SECOND;

        StringBuilder sbd = new StringBuilder();
        if (flag) {
            sbd.append("-");
        }
        if (year > 0) {
            sbd.append(year).append(hasCn ? " 年 " : (year > 1 ? " years " : " year "));
        }
        if (day > 0) {
            sbd.append(day).append(hasCn ? " 天 " : (day > 1 ? " days " : " day "));
        }
        if (hour > 0) {
            sbd.append(hour).append(hasCn ? " 小时 " : (hour > 1 ? " hours " : " hour "));
        }
        if (minute > 0) {
            sbd.append(minute).append(hasCn ? " 分 " : (minute > 1 ? " minutes " : " minute "));
        }
        if (second > 0) {
            sbd.append(second).append(hasCn ? " 秒 " : (second > 1 ? " seconds " : " second "));
        }
        if (m > 0) {
            sbd.append(m).append(hasCn ? " 毫秒" : " ms");
        }
        return sbd.toString().trim();
    }

    /** 获取一个日期所在天的最开始的时间(00:00:00 000), 对日期查询尤其有用 */
    public static LocalDateTime getDayStart(LocalDateTime date) {
        return U.isNull(date) ? null : date.with(LocalTime.MIN);
    }
    /** 获取一个日期所在天的最晚的时间(23:59:59 999), 对日期查询尤其有用 */
    public static LocalDateTime getDayEnd(LocalDateTime date) {
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
        return U.isNull(date) ? null : date.withMonth(date.get(IsoFields.QUARTER_OF_YEAR) * 3 - 2)
                .withDayOfMonth(1).with(LocalTime.MIN);
    }
    /** 获取一个日期所在季度的最后一毫秒(23:59:59 999) */
    public static LocalDateTime getQuarterEnd(LocalDateTime date) {
        return U.isNull(date) ? null : date.withMonth(date.get(IsoFields.QUARTER_OF_YEAR) * 3)
                .with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
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
