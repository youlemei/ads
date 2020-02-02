package com.lwz.ads.constant;

/**
 * @author liweizhou 2020/2/2
 */
public enum ClickStatusEnum {

    DISCARDED(-1), RECEIVED(0), UNCONVERTED(1), CONVERTED(2), DEDUCTED(3);

    int status;

    ClickStatusEnum(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
