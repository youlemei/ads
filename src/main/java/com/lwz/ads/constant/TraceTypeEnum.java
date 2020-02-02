package com.lwz.ads.constant;

public enum TraceTypeEnum {

    ASYNC("async"), REDIRECT("302");

    String type;

    TraceTypeEnum(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static TraceTypeEnum valueOfType(String type){
        for (TraceTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }

}
