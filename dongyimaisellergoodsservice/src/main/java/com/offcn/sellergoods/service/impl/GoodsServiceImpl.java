package com.offcn.sellergoods.service.impl;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.offcn.group.Goods;
import com.offcn.mapper.*;
import com.offcn.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.pojo.TbGoodsExample.Criteria;
import com.offcn.sellergoods.service.GoodsService;

import com.offcn.entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;

	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbBrandMapper brandMapper;

	@Autowired
	private TbSellerMapper sellerMapper;

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Override
	public List<TbItem> findGoodsIdsAndStatus(Long[] ids, String status) {

		TbItemExample example = new TbItemExample();
		TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdIn(Arrays.asList(ids));
		criteria.andStatusEqualTo(status);
		return itemMapper.selectByExample(example);
	}

	@Override
	public void updateStatus(Long[] ids, String status) {

		for (long id : ids) {
			//修改商品表
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setAuditStatus(status);
			goodsMapper.updateByPrimaryKey(goods);
			//修改sku规格表
			TbItemExample example = new TbItemExample();
			TbItemExample.Criteria criteria = example.createCriteria();
			criteria.andGoodsIdEqualTo(id);
			List<TbItem> itemList = itemMapper.selectByExample(example);
			for (TbItem tbItem : itemList) {
				tbItem.setStatus(status);
				itemMapper.updateByPrimaryKey(tbItem);
			}
		}

	}

	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		//1、新增商品表
		//修正商品审核状态未 未审核
		goods.getGoods().setAuditStatus("0");//0 商品未审核 1、表示审核
		goods.getGoods().setIsDelete("0");//0 表示未删除 1表示已删除
		goodsMapper.insert(goods.getGoods());
		//2、添加商品描述表 封装商品描述表的id 为商品id
		goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
		goodsDescMapper.insert(goods.getGoodsDesc());
        saveItemList(goods);
	}

	public void saveItemList(Goods goods){
        //若不使用规格 则将列表隐藏
        if("1".equals(goods.getGoods().getIsEnableSpec())){
            //3、添加不同规格的数据
            for (TbItem item : goods.getItemList()) {
                //标题
                String title = goods.getGoods().getGoodsName();
                Map<String,Object> map = JSON.parseObject(item.getSpec());
                for(Map.Entry entry : map.entrySet()){
                    title += entry.getValue() + " ";
                };
                System.out.println("title : "+ title);

                item.setTitle(title);
                setItemValues(goods,item);
                itemMapper.insert(item);
            }
        }else{
            //若没有规格 将库存 价格 补全 避免脏数据
            TbItem item=new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());//商品SPU+规格描述串作为SKU名称
            item.setPrice( goods.getGoods().getPrice() );//价格
            item.setStatus("0");//状态
            item.setIsDefault("0");//是否默认
            item.setNum(99999);//库存数量
            item.setSpec("{}");
            setItemValues(goods,item);
            itemMapper.insert(item);
        }
    }

	public void setItemValues(Goods goods,TbItem item){
		item.setGoodsId(goods.getGoods().getId());//商品SPU编号
		item.setSellerId(goods.getGoods().getSellerId());//商家编号
		item.setCategoryid(goods.getGoods().getCategory3Id());//商品分类编号（3级）
		item.setCreateTime(new Date());//创建日期
		item.setUpdateTime(new Date());//修改日期

		//查询品牌
		TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
		item.setBrand(brand.getName());
		//查卖家
		System.out.println("sellerId:"+goods.getGoods().getSellerId());
		TbSeller seller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
		item.setSeller(seller.getNickName());
		//查分类
		TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
		item.setCategory(itemCat.getName());
		//存图片
		List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(),Map.class);
		if(imageList.size() > 0){
			item.setImage(imageList.get(0).get("url")+"");
		}
	}
	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
        //修改的商品需要重新被审核
        goods.getGoods().setAuditStatus("0");
	    goodsMapper.updateByPrimaryKey(goods.getGoods());
	    goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
	    //修改sku 先删除 后新增
        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andGoodsIdEqualTo( goods.getGoods().getId());
        itemMapper.deleteByExample(example);

        saveItemList(goods);

	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
		//创建组对象
		Goods goods = new Goods();
		TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
		TbGoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(id);
		goods.setGoods(tbGoods);
		goods.setGoodsDesc(goodsDesc);

		//查询商品规格
		TbItemExample example = new TbItemExample();
		TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		List<TbItem> itemList = itemMapper.selectByExample(example);
		goods.setItemList(itemList);

		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			//逻辑
			TbGoods goods = goodsMapper.selectByPrimaryKey(id);
			goods.setIsDelete("1");//0表示未删除 1表示已删除
			goodsMapper.updateByPrimaryKey(goods);
		}

		List<TbItem> list = findGoodsIdsAndStatus(ids,"1");
		for (TbItem tbItem : list) {
			tbItem.setStatus("0");
			itemMapper.updateByPrimaryKey(tbItem);
		}
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		Criteria criteria = example.createCriteria();
		
		if(goods!=null){			
						if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
				criteria.andSellerIdEqualTo(goods.getSellerId());
			}			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
				criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
			}			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}

				criteria.andIsDeleteEqualTo(goods.getIsDelete());
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}
	
}
