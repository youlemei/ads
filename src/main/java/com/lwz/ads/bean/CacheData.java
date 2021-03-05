package com.lwz.ads.bean;

import lombok.Data;

/**
 * @author liweizhou 2021/3/5
 */
@Data
public class CacheData<T> {

    private T data;

    /**
     * nanos
     */
    private long duration;

}
