package com.lwz.ads.constant;

public enum PromoteStatusEnum {

    CREATING(0), RUNNING(1), STOPPED(2);

    int status;

    PromoteStatusEnum(int status){
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static PromoteStatusEnum valueOfStatus(int status){
        for (PromoteStatusEnum value : values()) {
            if (value.status == status) {
                return value;
            }
        }
        return null;
    }
}
