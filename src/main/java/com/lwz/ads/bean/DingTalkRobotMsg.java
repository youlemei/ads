package com.lwz.ads.bean;

import com.alibaba.fastjson.JSONObject;

/**
 * @author liweizhou 2020/7/11
 */
public class DingTalkRobotMsg extends JSONObject {

    private DingTalkRobotMsg(){}

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

        public DingTalkRobotMsg build(){
            DingTalkRobotMsg msg = new DingTalkRobotMsg();
            msg.put("msgtype", "text");
            msg.put("text", data);
            return msg;
        }

    }

}
