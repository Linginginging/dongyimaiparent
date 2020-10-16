package com.offcn.solrutil;

import com.alibaba.fastjson.JSON;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SolrUtil {

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private SolrTemplate solrTemplate;

    public static void main(String[] args) {

        ApplicationContext ac = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext-*.xml");
        SolrUtil su = ac.getBean("solrUtil",SolrUtil.class);
        su.importData();

    }

    public void importData(){
        //从mysql中取回集合数据
        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");
        List<TbItem> list = itemMapper.selectByExample(example);

        System.out.println("list:"+list.size());

        for (TbItem item : list) {

            Map specMap= JSON.parseObject(item.getSpec());//将spec字段中的json字符串转换为map
            item.setSpecMap(specMap);//给带注解的字段赋值

        }

        solrTemplate.saveBeans(list);
        solrTemplate.commit();

    }

}
