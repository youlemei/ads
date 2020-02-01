package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.service.IConvertRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.sampled.Line;

@Slf4j
@RestController
public class CallbackController {

    @Autowired
    private IConvertRecordService convertRecordService;

    @GetMapping("/callback")
    public Response callback(@RequestParam String date, @RequestParam String clickId){
        log.info("date:{} clickId:{}", date, clickId);
        if (date.length() != 8 || clickId.length() != 32) {
            return Response.with(HttpStatus.BAD_REQUEST);
        }
        convertRecordService.handleConvert(date, clickId);
        return Response.success();
    }

}
