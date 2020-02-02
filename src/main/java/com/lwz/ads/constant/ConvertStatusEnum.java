package com.lwz.ads.constant;

/**
 * @author liweizhou 2020/2/2
 */
public enum ConvertStatusEnum {

    RECEIVED(0), CONVERTED(1), NOTIFIED(2), DEDUCTED(3);

    int status;

    ConvertStatusEnum(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
