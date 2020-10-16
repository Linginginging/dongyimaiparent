package com.offcn.util;

import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.*;

@Component
public class SmsListener implements MessageListener {

    @Autowired
    private SmsUtil smsUtil;
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof MapMessage){
                MapMessage mapMessage = (MapMessage) message;
                System.out.println("发送短信");
                HttpResponse httpResponse = smsUtil.sendCode(mapMessage.getString("mobile"), mapMessage.getString("code"));
                System.out.println("响应状态码："+httpResponse.toString());

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
