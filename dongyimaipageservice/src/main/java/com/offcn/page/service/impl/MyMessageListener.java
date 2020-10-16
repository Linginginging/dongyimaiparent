package com.offcn.page.service.impl;

import com.offcn.page.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class MyMessageListener implements MessageListener {

    @Autowired
    private ItemPageService itemPageService;

    @Override
    public void onMessage(Message message) {

        try {

            TextMessage textMessage = (TextMessage) message;

            System.out.println("textMessage : " +textMessage.getText());

            String id = (String)textMessage.getText();

            System.out.println("id : " + id);

            itemPageService.genItemHtml(Long.parseLong(id));

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
