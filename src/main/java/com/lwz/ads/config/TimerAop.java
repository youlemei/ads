package com.lwz.ads.config;

import com.lwz.ads.util.MDCUtils;
import com.lwz.ads.util.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author liweizhou 2020/2/17
 */
@Slf4j
@Aspect
@Component
public class TimerAop {

    @Autowired
    private RedisUtils redisUtils;

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        String timerName = String.format("%s.%s", joinPoint.getTarget().getClass().getSimpleName(), joinPoint.getSignature().getName());
        String uuid = UUID.randomUUID().toString();

        boolean lock = false;
        MDCUtils.putContext("timer=" + joinPoint.getSignature().getName());
        try {
            lock = redisUtils.lock(timerName, uuid, 30);
            if (lock) {
                return joinPoint.proceed();
            }
            return null;
        } finally {
            MDCUtils.clearContext();
            if (lock) {
                redisUtils.unlock(timerName, uuid);
            }
        }
    }

}
