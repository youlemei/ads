package com.lwz.ads.config;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import com.lwz.ads.util.SmartRejectedExecutionHandler;
import io.lettuce.core.AbstractRedisClient;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.redis.RedisHealthIndicator;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author liweizhou 2020/2/2
 */
@Slf4j
@Configuration
public class MyConfig {

    @Value("${server.port}")
    private Integer port;

    @Value("${spring.profiles.active}")
    private String profile;

    @PostConstruct
    public void init(){
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        log.info("####################################################################################################");
        log.info("==========================>>    server start! port:{} profile:{}    <<==========================", port, profile);
        log.info("==========================>>    collectors:{}   <<========================",
                collectors.stream().map(MemoryManagerMXBean::getName).collect(Collectors.toList()));
        log.info("####################################################################################################");
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(redisTemplate.getStringSerializer());
        redisTemplate.setHashKeySerializer(redisTemplate.getStringSerializer());
        GenericFastJsonRedisSerializer fastJsonRedisSerializer = new GenericFastJsonRedisSerializer();
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);
        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
        return redisTemplate;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient.connectionPool(new ConnectionPool(20, 10, TimeUnit.MINUTES));
        OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient.build());
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 开启 count 的 join 优化,只针对部分 left join
        paginationInterceptor.setCountSqlParser(new JsqlParserCountOptimize(true));
        return paginationInterceptor;
    }

    @Bean
    public TaskDecorator taskDecorator() {
        return task -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                try {
                    task.run();
                } finally {
                    if (contextMap != null) {
                        MDC.clear();
                    }
                }
            };
        };
    }

    @Bean
    public TaskExecutorCustomizer taskExecutorCustomizer(RejectedExecutionHandler smartRejectedExecutionHandler) {
        return taskExecutor -> {
            taskExecutor.setRejectedExecutionHandler(smartRejectedExecutionHandler);
        };
    }

    @Bean
    public RejectedExecutionHandler smartRejectedExecutionHandler() {
        return new SmartRejectedExecutionHandler();
    }

    @Bean
    public RedisHealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory){
        return new RedisHealthIndicator(redisConnectionFactory) {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                super.doHealthCheck(builder);
                try {
                    Field clientField = ReflectionUtils.findField(LettuceConnectionFactory.class, "client", AbstractRedisClient.class);
                    clientField.setAccessible(true);
                    Object client = ReflectionUtils.getField(clientField, redisConnectionFactory);
                    Field eventLoopGroupsField = ReflectionUtils.findField(AbstractRedisClient.class, "eventLoopGroups", Map.class);
                    eventLoopGroupsField.setAccessible(true);
                    Map<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroups =
                            (Map<Class<? extends EventLoopGroup>, EventLoopGroup>) ReflectionUtils.getField(eventLoopGroupsField, client);
                    eventLoopGroups.keySet().forEach(clz -> log.info("Redis EventLoopGroup:{}", clz.getCanonicalName()));
                } catch (Exception e) {
                    log.warn("doHealthCheck fail. err:{}", e.getMessage(), e);
                }
            }
        };
    }

}
