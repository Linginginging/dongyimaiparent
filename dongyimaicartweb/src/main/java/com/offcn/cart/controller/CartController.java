package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.offcn.entity.Cart;
import com.offcn.entity.Result;
import com.offcn.service.CartService;

import com.offcn.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference(timeout = 30000)
    private CartService cartService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    /**
     * 购物车列表
     * @return
     */
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(){
        //得到登陆人账号,判断当前是否有人登陆
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

            String cartList = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
            if (cartList==null||cartList.equals("")){
                cartList="[]";
            }
            List<Cart> carts = JSON.parseArray(cartList, Cart.class);
        if (username.equals("anonymousUser")){//如果未登录
            return carts;
       }else {
           List<Cart> listFromRedis = cartService.findCartListFromRedis(username);
            if(carts.size()>0){//如果本地存在购物车，就合并购物车
                listFromRedis = cartService.mergeCartList(listFromRedis, carts);
                //清除本地cookie的缓存
                CookieUtil.deleteCookie(request,response,"cartList");
                //将合并后的数据存入redis
                cartService.saveCartListToRedis(listFromRedis,username);
            }
           return listFromRedis;
       }
    }

    /**
     * 添加商品到购物车
     * @param itemId
     * @param num
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(Long itemId,Integer num){
        try {

            response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");
            response.setHeader("Access-Control-Allow-Credentials", "true");


            //得到登陆人账号,判断当前是否有人登陆
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录用户："+username);

            List<Cart> cartList = findCartList();//获取购物车列表
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);
            if (username.equals("anonymousUser")){//如果未登录，保存到cookie


                try {
                    CookieUtil.setCookie(request,response,"cartList",JSON.toJSONString(cartList),3600*24,"UTF-8");
                    System.out.println("向cookie存入数据...");
                    return new Result(true,"添加成功");
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Result(false,"添加失败");
                }


            }else {//如果已登录，存入数据到redis
                cartService.saveCartListToRedis(cartList,username);
            }
            return new Result(true,"添加redis购物车成功");
      }catch (Exception e){
          e.printStackTrace();
          return new Result(false,"添加redis购物车失败");
      }

    }
}















