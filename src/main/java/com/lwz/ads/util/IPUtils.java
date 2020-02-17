package com.lwz.ads.util;

/**
 * @author liweizhou 2020/2/17
 */
public class IPUtils {

    public static boolean isLocalhost(String host){
        return "localhost".equalsIgnoreCase(host) || "2020funfantasy.cn".equalsIgnoreCase(host) || "47.107.70.137".equals(host);
    }

}
