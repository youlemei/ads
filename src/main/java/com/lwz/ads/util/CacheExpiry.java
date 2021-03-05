package com.lwz.ads.util;

import com.github.benmanes.caffeine.cache.Expiry;
import com.lwz.ads.bean.CacheData;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * @author liweizhou 2021/3/5
 */
public class CacheExpiry implements Expiry<String, CacheData> {
    @Override
    public long expireAfterCreate(String key, CacheData value, long currentTime) {
        return value.getDuration();
    }

    @Override
    public long expireAfterUpdate(String key, CacheData value, long currentTime, @NonNegative long currentDuration) {
        return value.getDuration();
    }

    @Override
    public long expireAfterRead(String key, CacheData value, long currentTime, @NonNegative long currentDuration) {
        return value.getDuration();
    }
}
