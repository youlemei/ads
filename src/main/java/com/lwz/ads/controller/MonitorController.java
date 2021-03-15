package com.lwz.ads.controller;

import com.lwz.ads.bean.Response;
import com.lwz.ads.service.MonitorService;
import com.lwz.ads.service.impl.ClickRecordServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author liweizhou 2021/3/13
 */
@RestController
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @Autowired
    private ClickRecordServiceImpl clickRecordService;

    @RequestMapping("/monitor")
    public Response retryConvert() {
        return Response.success(monitorService.monitorStat());
    }

    @RequestMapping("/refreshExecutor")
    public Response refreshExecutor(@RequestParam Long id) {
        return Response.success(clickRecordService.removeExecutor(id));
    }

}
