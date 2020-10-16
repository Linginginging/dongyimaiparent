package com.offcn.search.service.impl;


import com.alibaba.fastjson.JSON;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
import java.util.Map;

@Component
public class MyMessageListener implements MessageListener {

    @Autowired
    private ItemSearchService itemSearchService;

    @Override
    public void onMessage(Message message) {

        try {
            TextMessage textMessage = (TextMessage) message;
            String str = textMessage.getText();
            List<TbItem> list = JSON.parseArray(str,TbItem.class);
            for (TbItem tbItem : list) {

                Map specMap = JSON.parseObject(tbItem.getSpec());
                tbItem.setSpecMap(specMap);
            }

            itemSearchService.importList(list);
            System.out.println("solr 同步完成...");
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
