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
import java.util.Map;

/**
 * @author liweizhou 2020/2/13
 */
@Slf4j
public class WeChatAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private RestTemplate restTemplate;

    private HttpHeaders headers;

    public static final String URL = Const.WECHAT_ROBOT_URL_DEF;

    public static final String PORT = System.getenv().getOrDefault("SERVER_PORT", System.getProperty("SERVER_PORT", "9999"));

    public WeChatAppender() {
        restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void append(ILoggingEvent event) {

        String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault())
                .format(DateUtils.DEFAULT_FORMATTER);
        Map<String, String> mdc = event.getMDCPropertyMap();

        StringBuilder sb = new StringBuilder()
                .append(time).append(" ERROR ").append("[").append(PORT).append("] ")
                .append("[").append(mdc.getOrDefault("origin", "")).append(" ")
                .append(mdc.getOrDefault("trace", "")).append("]")
                .append(event.getLoggerName().substring(event.getLoggerName().lastIndexOf(".") + 1))
                .append(":").append(event.getCallerData()[0].getLineNumber()).append(" - ")
                .append(event.getFormattedMessage());

        if (event.getThrowableProxy() instanceof ThrowableProxy) {

            ThrowableProxy throwableProxy = (ThrowableProxy) event.getThrowableProxy();
            Throwable throwable = throwableProxy.getThrowable();
            sb.append("\n").append(throwable.getClass().getCanonicalName()).append(":").append(throwable.getMessage());

            StackTraceElementProxy[] stackTraceElementProxy = throwableProxy.getStackTraceElementProxyArray();
            //打印n行的堆栈
            for (int i = 0; i < 3 && i < stackTraceElementProxy.length; i++) {
                StackTraceElementProxy proxy = stackTraceElementProxy[i];
                sb.append("\n\t").append(proxy.getSTEAsString());
            }
        }


        notify(sb.toString());
    }

    public void notify(String err){
        try {
            WeChatRobotMsg msg = WeChatRobotMsg.buildText().content(err).build();
            String resp = restTemplate.postForObject(URL, new HttpEntity<>(msg, headers), String.class);
            log.info("notify ok. resp:{}", resp);
        } catch (Exception e) {
            log.warn("notify fail. err:{}, webhook:{}, msg:{}", e.getMessage(), URL, err, e);
        }
    }
}
