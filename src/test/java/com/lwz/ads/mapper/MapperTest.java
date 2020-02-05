package com.lwz.ads.mapper;

import com.lwz.Main;
import com.lwz.ads.entity.Company;
import com.lwz.ads.entity.PromoteRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class MapperTest {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private PromoteRecordMapper promoteRecordMapper;

    @Test
    public void testSelect() throws Exception {
        Company x = companyMapper.selectById(1L);
        System.out.println(x);
        x.setCompanyName(x.getCompanyName() + "1");
        companyMapper.updateById(x);
        System.out.println(companyMapper.selectById(1L));
        System.out.println(companyMapper.selectById(1L));
    }

    @Test
    public void testSaveConvert() throws Exception{
        PromoteRecord entity = new PromoteRecord().setAdId(1L).setChannelId(33L).setTraceType("302");
        promoteRecordMapper.insert(entity);
    }

}
