/*
 * @(#)Clock.java 2013-07-11
 * 
 * Copy Right@ 欢聚时代
 */

package com.lwz.ads.util;

public class Clock {

    private long begin;

    private long last;

    private StringBuilder buf;

    public Clock() {
        this.begin = System.currentTimeMillis();
        this.last = this.begin;
        this.buf = new StringBuilder();
    }

    public Clock tag() {
        long curr = System.currentTimeMillis();
        long span = curr - this.last;
        this.last = curr;
        buf.append("|").append(span).append("ms");
        return this;
    }

    @Override
    public String toString() {
        return String.format("@[clock:%sms%s]", last - begin, buf);
    }

}
