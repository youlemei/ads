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
     * redis-value click_day_limit_date:id
     */
    public static final String CLICK_DAY_LIMIT_KEY = "click_day_limit_%s:%s";

    /**
     * redis-value convert_day_limit_date:id
     */
    public static final String CONVERT_DAY_LIMIT_KEY = "convert_day_limit_%s:%s";

    public static final String ERROR_WEB_HOOK = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=db98068e-b60b-493c-826b-8936d814a7d1";

}
