package com.github.common.date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 日期的格式化类型 */
@Getter
@RequiredArgsConstructor
public enum DateFormatType {

    /** yyyy-MM-dd HH:mm:ss */
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    /** yyyy-MM-dd HH:mm */
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
    /** yyyy-MM-dd */
    YYYY_MM_DD("yyyy-MM-dd"),
    /** yyyy-MM */
    YYYY_MM("yyyy-MM"),
    /** HH:mm:ss */
    HH_MM_SS("HH:mm:ss"),
    /** HH:mm */
    HH_MM("HH:mm"),
    /** yyyy */
    YYYY("yyyy"),
    /** MM-dd */
    MM_DD("MM-dd"),

    /** yyyy-MM-dd HH:mm:ss SSS */
    YYYY_MM_DD_HH_MM_SS_SSS("yyyy-MM-dd HH:mm:ss SSS"),
    /** yyyy-MM-dd HH:mm:ss.SSS 到毫秒 */
    YYYY_MM_DD_HH_MM_SSSSS("yyyy-MM-dd HH:mm:ss.SSS"),
    /** yyyy-MM-dd HH:mm:ss.SSSSSS 到微秒 */
    YYYY_MM_DD_HH_MM_SSSSSSSS("yyyy-MM-dd HH:mm:ss.SSSSSS"),
    /** yyyy-MM-dd HH:mm:ss.SSSSSSSSS 到纳秒 */
    YYYY_MM_DD_HH_MM_SSSSSSSSSSS("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"),

    /** 到秒: yyyy-MM-ddTHH:mm:ssZ */
    TZ("yyyy-MM-dd'T'HH:mm:ss'Z'"),
    /** 到秒: yyyy-MM-ddTHH:mm:ss */
    T("yyyy-MM-dd'T'HH:mm:ss"),
    /** 到毫秒: yyyy-MM-ddTHH:mm:ss.SSSZ */
    TSZ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
    /** 到毫秒: yyyy-MM-ddTHH:mm:ss.SSS */
    TS("yyyy-MM-dd'T'HH:mm:ss.SSS"),

    /** yyyyMMddHHmmssSSS */
    YYYYMMDDHHMMSSSSS("yyyyMMddHHmmssSSS"),
    /** yyyyMMddHHmmss */
    YYYYMMDDHHMMSS("yyyyMMddHHmmss"),
    /** yyMMddHHmmss */
    YYMMDDHHMMSS("yyMMddHHmmss"),
    /** yyyyMMddHHmm */
    YYYYMMDDHHMM("yyyyMMddHHmm"),
    /** yyMMddHHmm */
    YYMMDDHHMM("yyMMddHHmm"),
    /** yyyyMMdd */
    YYYYMMDD("yyyyMMdd"),
    /** yyMMdd */
    YYMMDD("yyMMdd"),
    /** yyyyMM */
    YYYYMM("yyyyMM"),

    /** yyyy/MM/dd */
    USA_YYYY_MM_DD("yyyy/MM/dd"),
    /** yyyy/MM/dd */
    USA_YYYY_MM_DD_HH_MM("yyyy/MM/dd HH:mm"),
    /** yyyy/MM/dd */
    USA_YYYY_MM_DD_HH_MM_SS("yyyy/MM/dd HH:mm:ss"),
    /** MM/dd/yyyy HH:mm:ss */
    USA_MM_DD_YYYY_HH_MM("MM/dd/yyyy HH:mm"),
    /** MM/dd/yyyy HH:mm:ss */
    USA_MM_DD_YYYY_HH_MM_SS("MM/dd/yyyy HH:mm:ss"),

    /** yyyy年MM月dd日 HH时mm分ss秒 */
    CN_YYYY_MM_DD_HH_MM_SS("yyyy年MM月dd日HH时mm分ss秒"),
    /** yyyy年MM月dd日HH时mm分 */
    CN_YYYY_MM_DD_HH_MM("yyyy年MM月dd日HH时mm分"),
    /** yyyy年MM月dd日HH时 */
    CN_YYYY_MM_DD_HH("yyyy年MM月dd日HH时"),
    /** yyyy年MM月dd日 */
    CN_YYYY_MM_DD("yyyy年MM月dd日"),

    /** 直接打印 new Date() 时的样式 */
    CST("EEE MMM dd HH:mm:ss zzz yyyy");

    private final String value;

    public boolean isCst() {
        return this == CST;
    }

    public boolean isLocalDateTimeType() {
        return this == YYYY_MM_DD_HH_MM_SS || this == YYYY_MM_DD_HH_MM
                || this == YYYY_MM_DD_HH_MM_SS_SSS || this == YYYY_MM_DD_HH_MM_SSSSS
                || this == YYYY_MM_DD_HH_MM_SSSSSSSS || this == YYYY_MM_DD_HH_MM_SSSSSSSSSSS
                || this == TZ || this == T || this == TSZ || this == TS
                || this == YYYYMMDDHHMMSSSSS || this == YYYYMMDDHHMMSS || this == YYMMDDHHMMSS
                || this == YYYYMMDDHHMM || this == YYMMDDHHMM
                || this == USA_YYYY_MM_DD_HH_MM || this == USA_YYYY_MM_DD_HH_MM_SS
                || this == USA_MM_DD_YYYY_HH_MM || this == USA_MM_DD_YYYY_HH_MM_SS
                || this == CN_YYYY_MM_DD_HH_MM_SS || this == CN_YYYY_MM_DD_HH_MM
                || this == CN_YYYY_MM_DD_HH;
    }
    public boolean isLocalDateType() {
        return this == YYYY_MM_DD || this == YYYY_MM
                || this == YYYYMMDD || this == YYMMDD
                || this == YYYYMM || this == MM_DD || this == USA_YYYY_MM_DD;
    }
    public boolean isLocalTimeType() {
        return this == HH_MM_SS || this == HH_MM;
    }
    public boolean isYearType() {
        return this == YYYY;
    }
}
