package com.lwz.ads.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.lwz.Main;
import com.lwz.ads.constant.PromoteStatusEnum;
import com.lwz.ads.mapper.entity.PromoteRecord;
import com.lwz.ads.service.impl.PromoteRecordServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author liweizhou 2020/2/6
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@ActiveProfiles("dev")
public class PromoteRecordServiceTest {

    @Autowired
    private PromoteRecordServiceImpl promoteRecordService;

    @Test
    public void testList() throws Exception{
        LambdaQueryChainWrapper<PromoteRecord> queryWrapper = promoteRecordService.lambdaQuery()
                .eq(PromoteRecord::getPromoteStatus, PromoteStatusEnum.CREATING.getStatus());
        List<PromoteRecord> promoteRecordList = queryWrapper.list();
    }

}
