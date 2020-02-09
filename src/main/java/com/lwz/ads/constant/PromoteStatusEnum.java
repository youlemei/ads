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

}
