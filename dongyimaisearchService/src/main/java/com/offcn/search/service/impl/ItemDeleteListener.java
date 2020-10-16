package com.offcn.search.service.impl;

import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.Arrays;

@Component
public class ItemDeleteListener implements MessageListener {

    @Autowired
    private ItemSearchService itemSearchService;

    @Override
    public void onMessage(Message message) {

        try {

            ObjectMessage objectMessage = (ObjectMessage) message;

            Long[]  goodsIds = (Long[]) objectMessage.getObject();
            System.out.println("goodsIds : " + goodsIds);
            itemSearchService.deleteList(Arrays.asList(goodsIds));


        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
