package com.lwz.ads.service;

import com.lwz.Main;
import com.lwz.ads.service.impl.AdvertisementReportServiceImpl;
import com.lwz.ads.util.RedisUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

/**
 * @author liweizhou 2020/2/7
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@ActiveProfiles("dev")
public class AdvertisementReportServiceTest {

    @Autowired
    private AdvertisementReportServiceImpl advertisementReportService;

    @Autowired
    private RedisUtils redisUtils;

    @Test
    public void testLock() throws Exception{
        String uuid = UUID.randomUUID().toString();
        System.out.println(redisUtils.lock("lwz", uuid, 1000));
        System.out.println(redisUtils.lock("lwz", uuid, 1000));
        System.out.println(redisUtils.unlock("lwz", uuid));
    }

}