package com.lwz.ads.util;

import org.slf4j.MDC;

/**
 * @author liweizhou 2021/3/13
 */
public class MDCUtils {

    public static final void putContext(String origin) {
        MDC.put("origin", origin);
        MDC.put("trace", "traceId=" + TraceId.nextId());
    }

    public static void clearContext() {
        MDC.clear();
    }
}
