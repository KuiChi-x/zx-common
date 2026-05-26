package com.zx.common.base.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author ZhaoXu
 * @date 2023/11/5 13:17
 */
public class DateUtils {
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd H"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd H"),
            DateTimeFormatter.ofPattern("yyyy/M/d H")
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d")
    );

    public static final DateTimeFormatter BASE_DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");

    /**
     * 获取当前时间戳
     * @return
     */
    public static Long getNowTime() {
        LocalDateTime localDateTime = getNowDateTime();
        return convert2Time(localDateTime);
    }

    public static LocalDateTime getNowDateTime() {
        return LocalDateTime.now(getShZoneId());
    }

    public static Date getNowDate() {
        LocalDateTime nowDateTime = getNowDateTime();
        return convert2Date(nowDateTime);
    }

    /**
     * 获取上海时区
     * @return
     */
    public static ZoneId getShZoneId() {
        return ZoneId.of("Asia/Shanghai");
    }

    public static LocalDateTime getStartOfDayDateTime() {
        return LocalDate
                .now()
                .atStartOfDay()
                .atZone(getShZoneId())
                .toLocalDateTime();
    }

    public static Date convert2Date(LocalDateTime localDateTime) {
        ZoneId shZoneId = getShZoneId();
        Instant instant = localDateTime.atZone(shZoneId).toInstant();
        return Date.from(instant);
    }

    public static Long convert2Time(LocalDateTime localDateTime) {
        return convert2Date(localDateTime).getTime();
    }

    public static LocalDateTime convert2LocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), getShZoneId());
    }

    public static LocalDateTime convert2LocalDateTime(Long time) {
        Date date = new Date();
        return convert2LocalDateTime(date);
    }

    /**
     * 格式化时间戳，例: 1708241342208 -> 2024年02月18日 15:29:02
     * @param time
     * @return
     */
    public static String format(Long time) {
        Date date = new Date(Optional.ofNullable(time).orElse(0L));
        return format(date, null);
    }

    /**
     * 格式化时间戳，例: 1708241342208 -> 2024-02-18 15:29:02
     * @param time
     * @param dateTimeFormatter
     * @return
     */
    public static String format(Long time, DateTimeFormatter dateTimeFormatter) {
        Date date = new Date(Optional.ofNullable(time).orElse(0L));
        return format(date, dateTimeFormatter);
    }

    /**
     * 格式化日期，例: new Date(0) -> 1970-01-01 08:00:00
     * @param date
     * @return
     */
    public static String format(Date date) {
        return format(date, null);
    }

    /**
     * 格式化日期，例: new Date(0) -> 1970-01-01 08:00:00
     * @param date
     * @return
     */
    public static String format(Date date, DateTimeFormatter dateTimeFormatter) {
        if (Objects.isNull(date)) {
            date = new Date(0);
        }
        Instant instant = date.toInstant();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, getShZoneId());
        if (Objects.nonNull(dateTimeFormatter)) {
            return localDateTime.format(dateTimeFormatter);
        }
        for (DateTimeFormatter timeFormatter : DATE_TIME_FORMATTERS) {
            try {
                return localDateTime.format(timeFormatter);
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    /**
     * 解析日期
     * @param date 例：2024年02月18日 15:29:02
     * @return
     */
    public static LocalDateTime parse(String date) {
        return parse(date, null);
    }

    /**
     * 按照一定格式，解析日期
     * @param date
     * @param dateTimeFormatter
     * @return
     */
    public static LocalDateTime parse(String date, DateTimeFormatter dateTimeFormatter) {
        if (Objects.isNull(date)) {
            return null;
        }
        if (Objects.nonNull(dateTimeFormatter)) {
            return LocalDateTime.parse(date, dateTimeFormatter);
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(date, formatter);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static LocalDateTime parseDate(String date) {
        return parseDate(date, null);
    }

    public static LocalDateTime parseDate(String date, DateTimeFormatter dateTimeFormatter) {
        if (Objects.isNull(date)) {
            return null;
        }
        LocalDate localDate = null;
        if (Objects.nonNull(dateTimeFormatter)) {
            localDate = LocalDate.parse(date, dateTimeFormatter);
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                localDate = LocalDate.parse(date, formatter);
                break;
            } catch (Throwable ignored) {
            }
        }
        if (Objects.nonNull(localDate)) {
            return localDate.atStartOfDay();
        }
        return null;
    }
}
