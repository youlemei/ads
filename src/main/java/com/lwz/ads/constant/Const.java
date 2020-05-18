package com.lwz.ads.constant;

import java.util.regex.Pattern;

/**
 * @author liweizhou 2020/2/1
 */
public class Const {

    public static final String CALLBACK = "callback";

    public static final String IP = "ip";

    public static final String MAC = "mac";

    public static final String IDFA = "idfa";

    public static final String IMEI = "imei";

    /**
     * like {callback}
     */
    public static final Pattern PARAM_PATTERN = Pattern.compile("^\\{[a-zA-Z0-9]+}$");

    /**
     * redis-value click_day_limit_yyyyMMdd:pid
     */
    public static final String CLICK_DAY_LIMIT_KEY = "click_day_limit_%s:%s";

    /**
     * redis-value convert_day_limit_yyyyMMdd:pid
     */
    public static final String CONVERT_DAY_LIMIT_KEY = "convert_day_limit_%s:%s";

    /**
     * redis-value click_day_limit_yyyyMMdd:pid
     */
    public static final String AD_CLICK_DAY_LIMIT_KEY = "ad_click_day_limit_%s:%s";

    /**
     * redis-value convert_day_limit_yyyyMMdd:pid
     */
    public static final String AD_CONVERT_DAY_LIMIT_KEY = "ad_convert_day_limit_%s:%s";

    /**
     * redis-hash click_day_amount_yyyyMMdd {pid: amount}
     */
    public static final String CLICK_DAY_AMOUNT = "click_day_amount_%s";

    /**
     * redis-set click_day_actual_amount_yyyyMMdd:pid (ip+mac.hash)
     */
    public static final String CLICK_DAY_ACTUAL_AMOUNT = "click_day_actual_amount_%s:%s";

    /**
     * redis-hash convert_day_amount_yyyyMMdd {pid: amount}
     */
    public static final String CONVERT_DAY_AMOUNT = "convert_day_amount_%s";

    /**
     * redis-hash convert_day_actual_amount_yyyyMMdd {pid: amount}
     */
    public static final String CONVERT_DAY_ACTUAL_AMOUNT = "convert_day_actual_amount_%s";

    /**
     * redis-hash click_socket_time_out_minute_minute {companyId: count}
     */
    public static final String CLICK_SOCKET_TIME_OUT_MINUTE = "click_socket_time_out_minute_%s";

    public static final String ERROR_WEB_HOOK = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=db98068e-b60b-493c-826b-8936d814a7d1";

}
