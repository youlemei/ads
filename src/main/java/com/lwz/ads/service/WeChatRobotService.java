package com.lwz.ads.service;

import com.lwz.ads.bean.WeChatRobotMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author liweizhou 2019/12/4
 */
@Service
public class WeChatRobotService {

    private static final Logger log = LoggerFactory.getLogger(WeChatRobotService.class);

    @Autowired
    private RestTemplate restTemplate;

    private HttpHeaders headers;

    public WeChatRobotService(){
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public void notify(String webhook, WeChatRobotMsg msg){
        try {
            String resp = restTemplate.postForObject(webhook, new HttpEntity<>(msg, headers), String.class);
            // 成功: {"errcode":0,"errmsg":"ok"} 暂忽略失败
            // 一分钟只能发20次, 更好的做法是放到队列中限流执行
            log.info("notify ok. msg:{}, resp:{}", msg, resp);
        } catch (Exception e) {
            log.error("notify fail. err:{}, webhook:{}, msg:{}", e.getMessage(), webhook, msg, e);
        }
    }

}
