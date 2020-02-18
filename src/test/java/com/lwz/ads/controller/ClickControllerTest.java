package com.lwz.ads.controller;

import com.lwz.Main;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author liweizhou 2020/2/18
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
@ActiveProfiles("dev")
public class ClickControllerTest {

    @Autowired
    private ClickController clickController;

    @Test
    public void click() throws Exception{
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(clickController).build();

        mockMvc.perform(MockMvcRequestBuilders
                .get("/click")
                .param("adId", "1")
                .param("channelId", "1")
                .param("type", "async"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk());

    }
}