package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void importList(List<TbItem> list) {

        for (TbItem tbItem : list) {

            Map<String,String> specMap = JSON.parseObject(tbItem.getSpec(),Map.class);

            Map map = new HashMap();

            for(Map.Entry entry : specMap.entrySet()){
                map.put("item_spec_"+entry.getKey(),entry.getValue());
            }
            tbItem.setSpecMap(map);
        }

        solrTemplate.saveBeans(list);
        solrTemplate.commit();

    }

    @Override
    public void deleteList(List goodsIds) {
        System.out.println("list : " +goodsIds);
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsid").in(goodsIds);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }


    @Override
    public Map<String, Object> search(Map searchMap) {

        String keywords = (String) searchMap.get("keywords");
        searchMap.put("keywords", keywords.replace(" ", ""));

        Map<String,Object> map = new HashMap<String,Object>();

        map.putAll(searchList(searchMap));
        List categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);

        //假若用户选择了分类 那么按照用户选择的分类进行对redis的品牌和规格进行查询
        //如果没有选择分类 则按照分类的第一个排序分类 进行对品牌和规格的查询
        if(!"".equals(searchMap.get("category"))){
            map.putAll(searchBrandAndSpec(searchMap.get("category")+""));
        }else{
            //取查询商品的 第一个分类 所对应的品牌与规格
            if(categoryList.size()>0){
                map.putAll(searchBrandAndSpec(categoryList.get(0)+""));
            }
        }

//        //1、创建solr的搜索条件
//        Query query = new SimpleQuery("*:*");
//        //2、取得 keywords
//        Criteria criteria = new Criteria("item_keywords");
//        criteria = criteria.is(searchMap.get("keywords"));
//        query.addCriteria(criteria);
//
//        ScoredPage page = solrTemplate.queryForPage(query, TbItem.class);
//
//        map.put("rows",page.getContent());

        return map;
    }

    public Map<String,Object> searchBrandAndSpec(String category){
        Map<String,Object> map = new HashMap<String,Object>();
        //1、根据分类名称 取分类对应id
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        //2、根据 typeId 也就是 templateId 查询对应品牌列表和规格列表
        List brandList = (List)redisTemplate.boundHashOps("brandList").get(typeId);
        map.put("brandList",brandList);
        List specList = (List)redisTemplate.boundHashOps("specList").get(typeId);
        map.put("specList",specList);
        return map;
    }

    public List searchCategoryList(Map searchMap){

        List list = new ArrayList();
        //1、构建查询 对象
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //2、创建分组查询条件
        GroupOptions options = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(options);
        //3、执行查询
        GroupPage groupPage = solrTemplate.queryForGroupPage(query,TbItem.class);
        GroupResult<TbItem> groupResult = groupPage.getGroupResult("item_category");
        Page<GroupEntry<TbItem>> page = groupResult.getGroupEntries();
        List<GroupEntry<TbItem>> groupEntryList = page.getContent();
        for (GroupEntry<TbItem> tbItemGroupEntry : groupEntryList) {
            list.add(tbItemGroupEntry.getGroupValue());
        }
        return list;
    }

    //基本高亮查询
    public Map searchList(Map searchMap){
        //1.1 高亮查询
        Map map = new HashMap();
        //1、创建高亮查询对象
        SimpleHighlightQuery query = new SimpleHighlightQuery();

        //2、设置高亮属性
        HighlightOptions options = new HighlightOptions();
        options.addField("item_title");
        //3、设置高亮样式
        options.setSimplePrefix("<em style='color:red'>");
        options.setSimplePostfix("</em>");
        //4、将样式放入到查询对象中
        query.setHighlightOptions(options);

        //5、拼接查询条件
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);


        //2.1 拼接分类的过滤条件
        if(!"".equals(searchMap.get("category"))){
            Criteria criteria1 = new Criteria("item_category").is(searchMap.get("category"));
            FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
            query.addFilterQuery(filterQuery);
        }
        //3.1 拼接品牌的过滤条件
        if(!"".equals(searchMap.get("brand"))){
            Criteria criteria1 = new Criteria("item_brand").is(searchMap.get("brand"));
            FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
            query.addFilterQuery(filterQuery);
        }
        //4.1 拼接规格的过滤条件
        if(searchMap.get("spec")!=null){
            Map<String,String> specMap = (Map)searchMap.get("spec");
            for (Map.Entry<String,String> entry : specMap.entrySet()){
                Criteria criteria1 = new Criteria("item_spec_"+entry.getKey()).is(entry.getValue());
                FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
                query.addFilterQuery(filterQuery);
            }
        }
        //5.1 拼接价格的过滤条件
        if(!"".equals(searchMap.get("price"))){

            String price = (String)searchMap.get("price");
            String[] arr = price.split("-");
            //0-500
            if("0".equals(arr[0])){
                Criteria criteria1 = new Criteria("item_price").lessThanEqual(arr[1]);
                FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
                query.addFilterQuery(filterQuery);
            }else if("*".equals(arr[1])){
                Criteria criteria1 = new Criteria("item_price").greaterThanEqual(arr[0]);
                FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
                query.addFilterQuery(filterQuery);
            }else{
                //价格<较大值
                Criteria criteria1 = new Criteria("item_price").lessThanEqual(arr[1]);
                FilterQuery filterQuery = new SimpleFacetQuery(criteria1);
                query.addFilterQuery(filterQuery);
                //价格>较小值
                Criteria criteria2 = new Criteria("item_price").greaterThanEqual(arr[0]);
                FilterQuery filterQuery2 = new SimpleFacetQuery(criteria2);
                query.addFilterQuery(filterQuery2);
            }
        }

        //6.1 分页查询
        //首页
        Integer  pageNo = (Integer )searchMap.get("pageNo");
        if(pageNo == null){
            pageNo = 1;
        }

        Integer pageSize = (Integer) searchMap.get("pageSize");
        if(pageSize == null){
            pageSize = 10;
        }

//        第1页 0-9
//           2  10-19
//           3  20-29
//           4  30-39
        query.setOffset((pageNo-1)*pageSize);
        query.setRows(pageSize);

        //7.1 获取排序对象和排序属性
        String sortValue = (String)searchMap.get("sort");
        String sortFiled = (String)searchMap.get("sortFiled");
        if(sortFiled != null && sortFiled.length() > 0){
            if("ASC".equals(sortValue)){
                Sort sort = new Sort(Sort.Direction.ASC,"item_"+sortFiled);
                query.addSort(sort);
            }
            if("DESC".equals(sortValue)){
                Sort sort = new Sort(Sort.Direction.DESC,"item_"+sortFiled);
                query.addSort(sort);
            }
        }

        //6、执行查询
        HighlightPage page = solrTemplate.queryForHighlightPage(query,TbItem.class);

        //8、获取高亮集合入口
        List<HighlightEntry<TbItem>> highlightEntryList = page.getHighlighted();
        //9、遍历高亮集合
        for(HighlightEntry<TbItem> highlightEntry:highlightEntryList) {
            //获取基本数据对象
            TbItem tbItem = highlightEntry.getEntity();
            if (highlightEntry.getHighlights().size() > 0 && highlightEntry.getHighlights().get(0).getSnipplets().size() > 0) {
                List<HighlightEntry.Highlight> highlightList = highlightEntry.getHighlights();
                //高亮结果集合
                List<String> snipplets = highlightList.get(0).getSnipplets();
                //获取第一个高亮字段对应的高亮结果，设置到商品标题
                tbItem.setTitle(snipplets.get(0));
            }
        }

        map.put("rows",page.getContent());
        map.put("totalPages",page.getTotalPages());//总页数
        map.put("totalElements",page.getTotalElements());//总条数
        return map;
    }

}
