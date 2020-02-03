package com.lwz.ads.mapper;

import com.lwz.ads.constant.ClickStatusEnum;
import com.lwz.ads.entity.ClickRecord;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author liweizhou 2020/2/3
 */
public class ClickRecordMapperTest {
    private static ClickRecordMapper mapper;

    @BeforeClass
    public static void setUpMybatisDatabase() {
        SqlSessionFactory builder = new SqlSessionFactoryBuilder().build(ClickRecordMapperTest.class.getClassLoader().getResourceAsStream("mybatisTestConfiguration/ClickRecordMapperTestConfiguration.xml"));
        //you can use builder.openSession(false) to not commit to database
        mapper = builder.getConfiguration().getMapper(ClickRecordMapper.class, builder.openSession(true));
    }

    @Test
    public void testInsertWithDate() throws FileNotFoundException {
        ClickRecord clickRecord = new ClickRecord();
        clickRecord.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        clickRecord.setChannelId(1L);
        clickRecord.setAdId(1L);
        clickRecord.setChannelCreator("liweizhou");
        clickRecord.setAdCreator("liweizhou");
        clickRecord.setCreateTime(LocalDateTime.now());
        clickRecord.setTraceType("302");
        clickRecord.setIp("localhost");
        clickRecord.setMac("1111");
        clickRecord.setParamJson("{}");

        mapper.insertWithDate(clickRecord, "20200203");
    }

    @Test
    public void testSelectByIdWithDate() throws FileNotFoundException {
        ClickRecord clickRecord = mapper.selectByIdWithDate("bd2824be4ed84bfe9c89a0073ab0a91a", "20200203");
    }

    @Test
    public void testUpdateByIdWithDate() throws FileNotFoundException {
        ClickRecord clickRecord = new ClickRecord();
        clickRecord.setId("bd2824be4ed84bfe9c89a0073ab0a91a");
        clickRecord.setChannelId(2L);
        clickRecord.setAdId(2L);
        clickRecord.setChannelCreator("liweizhou");
        clickRecord.setAdCreator("liweizhou");
        clickRecord.setCreateTime(LocalDateTime.now());
        clickRecord.setTraceType("302");
        clickRecord.setIp("localhost");
        clickRecord.setMac("2222");
        clickRecord.setParamJson("{}");

        mapper.updateByIdWithDate(clickRecord, "20200203");
    }

    @Test
    public void testUpdateWithDate() throws FileNotFoundException {
        ClickRecord me = new ClickRecord();
        me.setId("bd2824be4ed84bfe9c89a0073ab0a91a");
        me.setClickStatus(ClickStatusEnum.RECEIVED.getStatus());

        ClickRecord to = new ClickRecord();
        to.setClickStatus(ClickStatusEnum.UNCONVERTED.getStatus());
        to.setEditor("system");
        to.setEditTime(LocalDateTime.now());

        mapper.updateWithDate(me, to, "20200203");
    }
}
