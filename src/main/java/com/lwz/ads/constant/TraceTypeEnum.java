package com.lwz.ads.constant;

public enum TraceTypeEnum {

    ASYNC("async"), REDIRECT("302");

    String code;

    TraceTypeEnum(String code){
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static TraceTypeEnum valueOfCode(String code){
        for (TraceTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("traceType code is wrong. " + code);
    }

}
