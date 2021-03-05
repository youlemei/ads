package com.lwz.ads.controller;

import com.lwz.ads.service.SysConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author liweizhou 2020/2/10
 */
@Slf4j
@Profile("dev")
@RestController
public class TestController {

    @Autowired
    private SysConfigLoader sysConfigLoader;

    @GetMapping("test")
    public String testUrlParam(@RequestParam Map<String, Object> param) {
        log.error("testUrlParam param:{}", param);
        return "success";
    }

}
