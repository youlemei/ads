package com.lwz.ads.bean;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author liweizhou 2019/12/4
 */
public class WeChatRobotMsg extends JSONObject{

    private WeChatRobotMsg(){}

    public static TextMsgBuilder buildText(){
        return new TextMsgBuilder();
    }

    public static class TextMsgBuilder {

        private JSONObject data = new JSONObject();

        private TextMsgBuilder(){
        }

        public TextMsgBuilder content(String content){
            data.put("content", content);
            return this;
        }

        public TextMsgBuilder mentionedList(List<String> mentionedList){
            data.put("mentioned_list", mentionedList);
            return this;
        }

        public TextMsgBuilder mentionedMobileList(List<String> mentionedMobileList){
            data.put("mentioned_mobile_list", mentionedMobileList);
            return this;
        }

        public WeChatRobotMsg build(){
            WeChatRobotMsg msg = new WeChatRobotMsg();
            msg.put("msgtype", "text");
            msg.put("text", data);
            return msg;
        }

    }

}
