package com.lwz.ads.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author liweizhou 2020/2/17
 */
@Slf4j
public class IPUtils {

    static {
        getIp();
    }

    public static boolean isLocalhost(String host){
        return "localhost".equalsIgnoreCase(host) || "2020funfantasy.cn".equalsIgnoreCase(host) || "47.107.70.137".equals(host);
    }

    private static String ip;

    public static String getIp() {
        if (ip == null) {
            getLocalIp();
        }
        return ip;
    }

    private static void getLocalIp() {
        List<String> ipList = getHostAddress();
        log.info("this machine all ip are: {}", ipList);
        if (!CollectionUtils.isEmpty(ipList)) {
            for (String tip : ipList) {
                if (tip.startsWith("127.0.0."))
                    continue;
                if (tip.startsWith("10."))
                    continue;
                ip = tip;
                return;
            }
            ip = ipList.get(0);
        }
    }

    private static List<String> getHostAddress() {
        Enumeration<NetworkInterface> nis;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
        List<String> ipList = new ArrayList<>();
        while (nis.hasMoreElements()) {
            NetworkInterface ni = nis.nextElement();
            if ("eth0".equalsIgnoreCase(ni.getDisplayName())) {
                Enumeration<NetworkInterface> subNis = ni.getSubInterfaces();
                while (subNis.hasMoreElements()) {
                    getHostAddress(ipList, subNis.nextElement());
                }
            }
            getHostAddress(ipList, ni);
        }
        return ipList;
    }

    private static void getHostAddress(List<String> eth0List, NetworkInterface subNi) {
        Enumeration<InetAddress> ips = subNi.getInetAddresses();
        while (ips.hasMoreElements()) {
            InetAddress inet = ips.nextElement();
            String ip = inet.getHostAddress();
            if (ip.indexOf(":") == -1) {
                // 不使用IPv6
                eth0List.add(ip);
            }
        }
    }

    public static String getRealIp(HttpServletRequest httpServletRequest) {
        String realIp = httpServletRequest.getHeader("X-Real-IP");
        if (notUnknownText(realIp)) {
            return realIp;
        }
        String forwardFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (notUnknownText(forwardFor)) {
            String[] ips = forwardFor.split(",");
            for (String ip : ips) {
                if (notUnknownText(ip)) {
                    return ip;
                }
            }
        }
        /*
        String proxyClientIp = httpServletRequest.getHeader("Proxy-Client-IP");
        if (notUnknownText(proxyClientIp)) {
            return proxyClientIp;
        }
        String WLProxyClientIp = httpServletRequest.getHeader("WL-Proxy-Client-IP");
        if (notUnknownText(WLProxyClientIp)) {
            return WLProxyClientIp;
        }
        */
        return httpServletRequest.getRemoteAddr();
    }

    private static boolean notUnknownText(String text){
        return StringUtils.hasText(text) && !"unknown".equalsIgnoreCase(text) && text.indexOf(":") == -1;
    }


    public static void main(String[] args) throws Exception {
        System.out.println(getIp());
    }

}
