package com.lwz.ads.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author liweizhou 2021/3/13
 */
public class TraceId {

    private static final AtomicLong traceId = new AtomicLong((long) (Math.random() * Integer.MAX_VALUE));

    public static long nextId() {
        return traceId.incrementAndGet();
    }

}
