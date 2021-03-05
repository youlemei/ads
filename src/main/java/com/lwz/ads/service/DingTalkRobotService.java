package com.lwz.ads.service;

import com.lwz.ads.bean.DingTalkRobotMsg;
import com.lwz.ads.constant.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author liweizhou 2019/12/4
 */
@Service
public class DingTalkRobotService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkRobotService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SysConfigLoader sysConfigLoader;

    private HttpHeaders headers;

    public DingTalkRobotService(){
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    public void notify(String webhook, DingTalkRobotMsg msg) {
        try {
            String url = sysConfigLoader.getString(webhook, Const.DING_ROBOT_URL_DEF);
            String resp = restTemplate.postForObject(url, new HttpEntity<>(msg, headers), String.class);
            // 成功: {"errcode":0,"errmsg":"ok"} 暂忽略失败
            // 一分钟只能发20次, 更好的做法是放到队列中限流执行
            log.info("notify ok. msg:{}, resp:{}", msg, resp);
        } catch (RestClientException e) {
            log.error("notify fail. err:{}, webhook:{}, msg:{}", e.getMessage(), webhook, msg, e);
        }
    }

}
