package com.lwz.ads.service;

import com.lwz.Main;
import com.lwz.ads.service.impl.AdvertisementReportServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author liweizhou 2020/2/7
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class AdvertisementReportServiceTest {

    @Autowired
    private AdvertisementReportServiceImpl advertisementReportService;

    @Test
    public void test() throws Exception{
        advertisementReportService.countAdReport();
    }

}