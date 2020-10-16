package com.offcn.service;

import com.offcn.entity.Cart;

import java.util.List;

public interface CartService {

    /**
     * 从redis中查询购物车
     * @param username
     */
    public List<Cart> findCartListFromRedis(String username);

    public void saveCartListToRedis(List<Cart> cartList,String username);

    /**
     * 添加商品到购物车
     * @param itemId
     * @param num
     * @return
     */
    public List<Cart> addGoodsToCartList(List<Cart> cartList,Long itemId,Integer num);

    /**
     * 合并redis购物车和cookie购物车
     * @param cartList1
     * @param cartList2
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart> cartList2);
}
