package com.lwz.ads.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author liweizhou 2020/2/10
 */
@Slf4j
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev")
@RestController
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("test")
    public String testUrlParam(@RequestParam Map<String, Object> param) {
        log.info("testUrlParam param:{}", param);
        System.out.println(restTemplate.getForObject(param.get("url1").toString(), String.class));
        System.out.println(restTemplate.getForObject(param.get("url2").toString(), String.class));
        return "success";
    }

}
