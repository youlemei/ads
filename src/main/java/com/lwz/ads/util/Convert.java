package com.lwz.ads.util;

import org.springframework.util.StringUtils;

/**
 * @author liweizhou 2020/4/11
 */
public class Convert {

    public static long toLong(Object o){
        if (StringUtils.isEmpty(o)) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            return Long.parseLong(o.toString().trim());
        }
        throw new IllegalArgumentException("can not convert to long. " + o);
    }

    public static int toInt(Object o){
        if (StringUtils.isEmpty(o)) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        if (o instanceof String) {
            return Integer.parseInt(o.toString().trim());
        }
        throw new IllegalArgumentException("can not convert to int. " + o);
    }

}
