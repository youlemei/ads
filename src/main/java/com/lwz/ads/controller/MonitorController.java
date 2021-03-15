package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liweizhou 2021/3/13
 */
@RestController
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @RequestMapping("/monitor")
    public Response retryConvert() {
        return Response.success(monitorService.monitorStat());
    }

}
