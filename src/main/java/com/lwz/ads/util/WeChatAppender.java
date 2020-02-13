package com.lwz.ads.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.lwz.ads.bean.WeChatRobotMsg;
import com.lwz.ads.constant.Const;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author liweizhou 2020/2/13
 */
@Slf4j
public class WeChatAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private RestTemplate restTemplate;

    private HttpHeaders headers;

    public WeChatAppender() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void append(ILoggingEvent event) {

        String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault()).format(DateUtils.DEFAULT_FORMATTER);
        //TODO: 获取服务器Ip，告知哪台服务器抛异常
        StringBuilder sb = new StringBuilder()
                .append("[").append(time).append("] ")
                .append("[").append(event.getLoggerName()).append("]:")
                .append(event.getCallerData()[0].getLineNumber()).append(" - ")
                .append(event.getFormattedMessage());

        if (event.getThrowableProxy() instanceof ThrowableProxy) {

            ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
            Throwable throwable = throwableProxy.getThrowable();
            sb.append("\n").append(throwable.getClass().getCanonicalName()).append(":").append(throwable.getMessage());

            StackTraceElementProxy[] stackTraceElementProxy = throwableProxy.getStackTraceElementProxyArray();
            //只打印10行的堆栈
            for (int i = 0; i < 10 && i < stackTraceElementProxy.length; i++) {
                StackTraceElementProxy proxy = stackTraceElementProxy[i];
                sb.append("\n\t").append(proxy.getSTEAsString());
            }
        }

        notify(Const.ERROR_WEB_HOOK, WeChatRobotMsg.buildText().content(sb.toString()).build());
    }

    public void notify(String webhook, WeChatRobotMsg msg){
        try {
            String resp = restTemplate.postForObject(webhook, new HttpEntity<>(msg, headers), String.class);
            log.info("notify ok. resp:{}", resp);
        } catch (Exception e) {
            log.warn("notify fail. err:{}, webhook:{}, msg:{}", e.getMessage(), webhook, msg, e);
        }
    }
}
