package com.lwz.ads.util;

import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final DateTimeFormatter yyyyMMdd_HHmm = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    public static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

}
