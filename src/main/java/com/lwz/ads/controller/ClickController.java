package com.lwz.ads.controller;

import com.alibaba.fastjson.JSONObject;
import com.lwz.ads.bean.Response;
import com.lwz.ads.service.IClickRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
public class ClickController {

    @Autowired
    private IClickRecordService clickRecordService;

    @GetMapping("/click")
    public Response click(@RequestParam Long adId, @RequestParam Long channelId, @RequestParam Map<String, Object> request){
        request.remove("adId");
        request.remove("channelId");
        log.info("adId:{} channelId:{} request:{}", adId, channelId, request);
        if (adId <= 0 || channelId <= 0) {
            return Response.with(HttpStatus.BAD_REQUEST);
        }
        clickRecordService.handleClick(adId, channelId, request);
        return Response.success();
    }

}
