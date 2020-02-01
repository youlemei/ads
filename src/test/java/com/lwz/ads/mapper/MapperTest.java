package com.lwz.ads.mapper;

import com.lwz.Main;
import com.lwz.ads.entity.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class MapperTest {

    @Autowired
    private ChannelMapper channelMapper;

    @Test
    public void testSelect() throws Exception {
        List<Channel> channels = channelMapper.selectList(null);
    }

}
