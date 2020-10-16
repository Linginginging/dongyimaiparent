package com.offcn.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.entity.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import com.offcn.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 从redis中查询购物车
     * @param username
     * @return
     */
    @Override
    public List<Cart> findCartListFromRedis(String username) {
        System.out.println("从redis中获取购物车数据==="+username);
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
        if (cartList==null){
            cartList=new ArrayList();
        }
        return cartList;
    }

    @Override
    public void saveCartListToRedis(List<Cart> cartList, String username) {
        System.out.println("向redis中存入购物车数据。。。。"+username);
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
        //1.根据商品SKU ID查询SKU商品信息
        TbItem tbItem = itemMapper.selectByPrimaryKey(itemId);
        if (tbItem==null){
            throw new RuntimeException("商品不存在");
        }
        if (!tbItem.getStatus().equals("1")){
            throw new RuntimeException("商品无效");
        }
        //2.获取商家ID
        String sellerId = tbItem.getSellerId();

        //3.根据商家ID判断购物车列表中是否存在该商家的购物车
        Cart cart = searchCartBysellerId(cartList,sellerId);
        //4.如果购物车列表中不存在该商家的购物车
        if (cart==null){
            //新建购物车对象
             cart = new Cart();
             cart.setSellerId(sellerId);
             cart.setSellerName(tbItem.getSeller());
             TbOrderItem orderItem = createOrderItem(tbItem,num);
             List orderItemList = new ArrayList();
             orderItemList.add(orderItem);
             cart.setOrderItemList(orderItemList);
             //将购物车对象添加到购物车列表
            cartList.add(cart);


        }else {
            //5.如果购物车列表中存在该商家的购物车
            // 查询购物车明细列表中是否存在该商品
            TbOrderItem orderItem =  searchOrderItemByItemId(cart.getOrderItemList(),itemId);
            if (orderItem==null){
                //5.1. 如果没有，新增购物车明细
               orderItem = createOrderItem(tbItem,num);
               cart.getOrderItemList().add(orderItem);
            }else {
                //5.2. 如果有，在原购物车明细上添加数量，更改金额
                orderItem.setNum(orderItem.getNum()+num);
                orderItem.setTotalFee(new BigDecimal(orderItem.getNum()*orderItem.getPrice().doubleValue()));
                //如果数量操作后小于等于0，则移除
                if (orderItem.getNum()<=0){
                    cart.getOrderItemList().remove(orderItem);
                }
                //如果移除后的cart明细为0，则将cart移除
                if (cart.getOrderItemList().size()==0){
                    cartList.remove(cart);
                }
            }
        }


        return cartList;
    }

    /**
     * 合并cookie购物车和redis购物车
     * @param cartList1
     * @param cartList2
     * @return
     */
    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
        System.out.println("合并购物车");
        for (Cart cart : cartList2) {
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                cartList1 = addGoodsToCartList(cartList1, orderItem.getItemId(), orderItem.getNum());
            }
        }
        return cartList1;
    }

    /**
     * 根据商品Id查询购物车明细
     * @param orderItemList
     * @param itemId
     * @return
     */
    private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
        for (TbOrderItem orderItem : orderItemList) {
            if (orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }
        return null;
    }

    /**
     * 创建订单明细
     * @param tbItem
     * @param num
     * @return
     */
    private TbOrderItem createOrderItem(TbItem tbItem, Integer num) {

        if (num<=0){
            throw new RuntimeException("商品数量非法");
        }
        TbOrderItem orderItem = new TbOrderItem();
        orderItem.setGoodsId(tbItem.getGoodsId());
        orderItem.setItemId(tbItem.getId());
        orderItem.setNum(num);
        orderItem.setPicPath(tbItem.getImage());
        orderItem.setPrice(tbItem.getPrice());
        orderItem.setSellerId(tbItem.getSellerId());
        orderItem.setTitle(tbItem.getTitle());
        orderItem.setTotalFee(new BigDecimal(tbItem.getPrice().doubleValue()*num));
        return orderItem;
    }

    /**
     * 根据商家Id查询购物车对象
     * @param cartList
     * @param sellerId
     * @return
     */
    private Cart searchCartBysellerId(List<Cart> cartList, String sellerId) {
        for (Cart cart : cartList) {
            if (cart.getSellerId().equals(sellerId)){
                return cart;
            }
        }
        return null;
    }
}
