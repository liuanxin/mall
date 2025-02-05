package com.github.common.date;

import com.github.common.util.U;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = 365 * DAY;

    /** 到秒的时间戳(如 MySQL 的 UNIX_TIMESTAMP() 函数) */
    public static Date toDate(int timestamp) {
        return new Date(timestamp * 1000L);
    }

    /** 当前时间 */
    public static Date now() {
        return new Date();
    }

    /** 返回 yyyy-MM-dd HH:mm:ss 格式的当前时间 */
    public static String nowDateTime() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 返回 yyyy-MM-dd HH:mm:ss SSS 格式的当前时间 */
    public static String nowDateTimeMs() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
    }
    /** 获取当前时间日期的字符串 */
    public static String now(DateFormatType dateFormatType) {
        return format(now(), dateFormatType);
    }
    /** 格式化日期 yyyy-MM-dd */
    public static String formatDate(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD);
    }
    /** 格式化日期 yyyy/MM/dd */
    public static String formatUsaDate(Date date) {
        return format(date, DateFormatType.USA_YYYY_MM_DD);
    }
    /** 格式化时间 HH:mm:ss */
    public static String formatTime(Date date) {
        return format(date, DateFormatType.HH_MM_SS);
    }
    /** 格式化日期和时间 yyyy-MM-dd HH:mm:ss */
    public static String formatDateTime(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 格式化日期 yyyy-MM-dd HH:mm:ss SSS */
    public static String formatDateTimeMs(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SSSSS);
    }

    /** 格式化日期对象成字符串 */
    public static String format(Date date, DateFormatType type) {
        return (U.isNull(date) || U.isNull(type)) ? U.EMPTY : format(date, type.getValue());
    }

    public static String format(Date date, String type) {
        return (U.isNull(date) || U.isBlank(type)) ? U.EMPTY : DateTimeFormat.forPattern(type).print(date.getTime());
    }

    /**
     * 将字符串转换成 Date 对象
     *
     * @see DateFormatType
     */
    public static Date parse(String source) {
        if (U.isNotBlank(source)) {
            for (DateFormatType type : DateFormatType.values()) {
                Date date = parse(source, type);
                if (U.isNotNull(date)) {
                    return date;
                }
            }
        }
        return null;
    }
    public static Date parse(String source, DateFormatType type) {
        if (U.isNotBlank(source) && U.isNotNull(type)) {
            if (type.isCst()) {
                try {
                    // cst 单独处理
                    return new SimpleDateFormat(type.getValue(), Locale.ENGLISH).parse(source.trim());
                } catch (ParseException | IllegalArgumentException ignore) {
                    return null;
                }
            }
            return parse(source, type.getValue());
        }
        return null;
    }
    public static Date parse(String source, String type) {
        if (U.isNotBlank(source)) {
            try {
                Date date = DateTimeFormat.forPattern(type).parseDateTime(source.trim()).toDate();
                if (date != null) {
                    return date;
                }
            } catch (IllegalArgumentException ignore) {
            }
        }
        return null;
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
        boolean hasCn = LocaleContextHolder.getLocale() == Locale.CHINA;
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
        boolean hasCn = LocaleContextHolder.getLocale().equals(Locale.CHINA);

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
    public static Date getDayStart(Date date) {
        return U.isNull(date) ? null : getDateTimeStart(date).toDate();
    }
    private static DateTime getDateTimeStart(Date date) {
        return new DateTime(date)
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
    }
    /** 获取一个日期所在天的最晚的时间(23:59:59 999), 对日期查询尤其有用 */
    public static Date getDayEnd(Date date) {
        return U.isNull(date) ? null : getDateTimeEnd(date).toDate();
    }
    private static DateTime getDateTimeEnd(Date date) {
        return new DateTime(date)
                .hourOfDay().withMaximumValue()
                .minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue()
                .millisOfSecond().withMaximumValue();
    }

    /** 获取一个日期所在星期天(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static Date getSundayStart(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .plusWeeks(-1)
                        .dayOfWeek().withMaximumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在星期六(星期六是最后一天)的最后一毫秒(23:59:59 999) */
    public static Date getSaturdayEnd(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(6)
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在星期一(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static Date getMondayStart(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(1)
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在星期天(星期天是最后一天)的最后一毫秒(23:59:59 999) */
    public static Date getSundayEnd(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(7)
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在月的第一毫秒(00:00:00 000) */
    public static Date getMonthStart(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .dayOfMonth().withMinimumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在月的最后一毫秒(23:59:59 999) */
    public static Date getMonthEnd(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .dayOfMonth().withMaximumValue()
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在季度的第一毫秒(00:00:00 000) */
    public static Date getQuarterStart(Date date) {
        if (U.isNull(date)) {
            return null;
        }

        DateTime dateTime = new DateTime(date);
        int month = dateTime.getMonthOfYear();
        // 日期所在月的季度开始月
        int quarterMonth = (month % 3 != 0) ? ((month / 3) * 3 + 1) : (month - 2);

        return dateTime.monthOfYear().setCopy(quarterMonth)
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在季度的最后一毫秒(23:59:59 999) */
    public static Date getQuarterEnd(Date date) {
        if (U.isNull(date)) {
            return null;
        }
        DateTime dateTime = new DateTime(date);
        int month = dateTime.getMonthOfYear();
        // 日期所在月的季度结束月
        int quarterMonth = (month % 3 != 0) ? (((month / 3) + 1) * 3) : month;

        return dateTime.monthOfYear().setCopy(quarterMonth)
                .dayOfMonth().withMaximumValue()
                .hourOfDay().withMaximumValue()
                .minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue()
                .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在年的第一毫秒(23:59:59 999) */
    public static Date getYearStart(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .monthOfYear().withMinimumValue()
                        .dayOfMonth().withMinimumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在年的最后一毫秒(23:59:59 999) */
    public static Date getYearEnd(Date date) {
        return U.isNull(date) ? null :
                new DateTime(date)
                        .monthOfYear().withMaximumValue()
                        .dayOfMonth().withMaximumValue()
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /**
     * 取得指定日期 N 天后的日期
     *
     * @param year 正数表示多少年后, 负数表示多少年前
     */
    public static Date addYears(Date date, int year) {
        return new DateTime(date).plusYears(year).toDate();
    }
    /**
     * 取得指定日期 N 个月后的日期
     *
     * @param month 正数表示多少月后, 负数表示多少月前
     */
    public static Date addMonths(Date date, int month) {
        return new DateTime(date).plusMonths(month).toDate();
    }
    /**
     * 取得指定日期 N 天后的日期
     *
     * @param day 正数表示多少天后, 负数表示多少天前
     */
    public static Date addDays(Date date, int day) {
        return new DateTime(date).plusDays(day).toDate();
    }
    /**
     * 取得指定日期 N 周后的日期
     *
     * @param week 正数表示多少周后, 负数表示多少周前
     */
    public static Date addWeeks(Date date, int week) {
        return new DateTime(date).plusWeeks(week).toDate();
    }
    /**
     * 取得指定日期 N 小时后的日期
     *
     * @param hour 正数表示多少小时后, 负数表示多少小时前
     */
    public static Date addHours(Date date, int hour) {
        return new DateTime(date).plusHours(hour).toDate();
    }
    /**
     * 取得指定日期 N 分钟后的日期
     *
     * @param minute 正数表示多少分钟后, 负数表示多少分钟前
     */
    public static Date addMinute(Date date, int minute) {
        return new DateTime(date).plusMinutes(minute).toDate();
    }
    /**
     * 取得指定日期 N 秒后的日期
     *
     * @param second 正数表示多少秒后, 负数表示多少秒前
     */
    public static Date addSeconds(Date date, int second) {
        return new DateTime(date).plusSeconds(second).toDate();
    }

    /** 传入的时间是不是当月当日. 用来验证生日 */
    public static boolean wasBirthday(Date date) {
        DateTime dt = DateTime.now();
        DateTime dateTime = new DateTime(date);
        return dt.getMonthOfYear() == dateTime.getMonthOfYear() && dt.getDayOfMonth() == dateTime.getDayOfMonth();
    }

    /** 计算两个日期之间相差的年数. 如果 start 比 end 大将会返回负数 */
    public static int betweenYear(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 月、日、时、分、秒、毫秒 置为相同
        DateTime begin = new DateTime(start)
                .monthOfYear().withMinimumValue()
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end)
                .monthOfYear().withMinimumValue()
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        return Years.yearsBetween(begin, after).getYears();
    }

    /** 计算两个日期之间相差的月数. 如果 start 比 end 大将会返回负数 */
    public static int betweenMonth(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 日、时、分、秒、毫秒 置为相同
        DateTime begin = new DateTime(start)
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end)
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        return Months.monthsBetween(begin, after).getMonths();
    }

    /** 计算两个日期之间相差的星期数. 如果 start 比 end 大将会返回负数 */
    public static int betweenWeek(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将时间置为当前时间所在星期中<星期一>且将 时、分、秒、毫秒 置为相同
        DateTime begin = new DateTime(start)
                .dayOfWeek().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end)
                .dayOfWeek().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        return Weeks.weeksBetween(begin, after).getWeeks();
    }

    /** 计算两个日期之间相差的天数. 如果 start 比 end 大将会返回负数 */
    public static int betweenDay(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 时、分、秒、毫秒 置为相同
        DateTime begin = new DateTime(start)
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end)
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        return Days.daysBetween(begin, after).getDays();
    }

    /** 计算两个日期之间相差的小时数. 如果 start 比 end 大将会返回负数 */
    public static int betweenHour(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 分、秒、毫秒 置为相同
        DateTime begin = new DateTime(start)
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end)
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
        return Hours.hoursBetween(begin, after).getHours();
    }

    /** 计算两个日期之间相差的小时数. 如果 start 比 end 大将会返回负数 */
    public static int betweenMinute(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 秒、毫秒 置为相同
        DateTime begin = new DateTime(start).secondOfMinute().withMinimumValue().millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end).secondOfMinute().withMinimumValue().millisOfSecond().withMinimumValue();
        return Minutes.minutesBetween(begin, after).getMinutes();
    }

    /** 计算两个日期之间相差的秒数. 如果 start 比 end 大将会返回负数 */
    public static int betweenSecond(Date start, Date end) {
        if (U.isNull(start) || U.isNull(end)) {
            return 0;
        }
        // 将 毫秒 置为相同
        DateTime begin = new DateTime(start).millisOfSecond().withMinimumValue();
        DateTime after = new DateTime(end).millisOfSecond().withMinimumValue();
        return Seconds.secondsBetween(begin, after).getSeconds();
    }
}
