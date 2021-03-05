package com.lwz.ads.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.lwz.ads.mapper.SysConfigMapper;
import com.lwz.ads.mapper.entity.SysConfig;
import com.lwz.ads.util.Convert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;

/**
 * @author liweizhou 2021/3/5
 */
@Service
public class SysConfigLoader {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    private LoadingCache<String, Optional<String>> loadingCache;

    @PostConstruct
    public void init() {
        loadingCache = Caffeine.newBuilder()
                .initialCapacity(16)
                .maximumSize(1024)
                .recordStats()
                .refreshAfterWrite(Duration.ofSeconds(10))
                .build(key -> {
                    SysConfig sysConfig = ChainWrappers.lambdaQueryChain(sysConfigMapper).eq(SysConfig::getName, key).one();
                    return Optional.ofNullable(sysConfig == null ? null : sysConfig.getValue());
                });
    }

    public boolean getBoolean(String key, boolean def) {
        Optional<String> optional = loadingCache.get(key);
        if (optional.isPresent()) {
            return "true".equalsIgnoreCase(optional.get()) || "1".equals(optional.get());
        }
        return def;
    }

    public int getInt(String key, int def) {
        Optional<String> optional = loadingCache.get(key);
        if (optional.isPresent()) {
            return Convert.toInt(optional.get());
        }
        return def;
    }

    public String getString(String key, String def) {
        Optional<String> optional = loadingCache.get(key);
        return optional.isPresent() ? optional.get() : def;
    }

    public Optional<String> getString(String key) {
        return loadingCache.get(key);
    }

    public <T> Optional<T> getJSON(String key, Class<T> clz) {
        Optional<String> optional = loadingCache.get(key);
        if (optional.isPresent()) {
            return Optional.ofNullable(JSON.parseObject(optional.get(), clz));
        }
        return Optional.empty();
    }

    public CacheStats monitor() {
        return loadingCache.stats();
    }

}
