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

    public static final String SIGN = "sign";

    public static final String DT = "dt";

    public static final String TS = "ts";

    public static final String TMS = "tms";

    public static final String CLICK_ID = "click_id";

    public static final String URL = "url";


    /**
     * like {callback}
     */
    public static final Pattern PARAM_PATTERN = Pattern.compile("^\\{.+}$");

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

    /**
     * redis-hash click_cost_total_stat_yyyyMMdd 不过期, 点击耗时监控
     */
    public static final String CLICK_COST_TOTAL_STAT = "click_cost_total_stat_%s";


    public static final String WECHAT_ROBOT_URL = "wechat_robot_url";

    public static final String DING_ROBOT_URL = "ding_robot_url";

    public static final String WECHAT_ROBOT_URL_DEF = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=53806d20-0e67-4f80-b57b-33e161389a1a";

    public static final String DING_ROBOT_URL_DEF = "https://oapi.dingtalk.com/robot/send?access_token=6ecdcd9fcf81adb32967d720e83b24f9c57bfb09456c1bb63a528c3c7e98a242";

}
