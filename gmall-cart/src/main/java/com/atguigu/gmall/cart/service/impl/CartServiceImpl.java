package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "gmall:cart:";

    private static final String PRICE_PREFIX = "gmall:sku:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsApi pmsApi;

    @Autowired
    private GmallSmsApi smsApi;

    @Autowired
    private GmallWmsApi wmsApi;

    @Override
    public void addCart(Cart cart) {

        String key = getLoginState();

        //获取购物车,获取的是用户的hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 判断要添加的商品购物车是否存在
        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        if(hashOps.hasKey(skuId)){
            //有,更新数量
            //获取购物车中的sku记录
            String cartJson = hashOps.get(skuId).toString();
            //反序列化,更新数量
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount() + count);
            //重新写入redis
            hashOps.put(skuId,JSON.toJSONString(cart));

        }else {
            //没有,新增
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsApi.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if(skuInfoEntity == null){
                return;
            }
            cart.setCheck(true);
            cart.setTitle(skuInfoEntity.getSkuTitle());
            cart.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
            cart.setPrice(skuInfoEntity.getPrice());

            Resp<List<SkuSaleAttrValueEntity>> skuSaleAttrValuesResp = this.pmsApi.querySkuSaleAttrValuesBySkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = skuSaleAttrValuesResp.getData();
            cart.setSaleAttrValues(skuSaleAttrValueEntities);

            Resp<List<SaleVO>> salesResp = this.smsApi.querySkuSalesBySkuId(cart.getSkuId());
            List<SaleVO> saleVOS = salesResp.getData();
            cart.setSales(saleVOS);

            Resp<List<WareSkuEntity>> wareResp = this.wmsApi.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResp.getData();
            if(CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
            //同步价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuInfoEntity.getPrice().toString());

        }
        hashOps.put(skuId,JSON.toJSONString(cart));

    }

    private String getLoginState() {
        String key = KEY_PREFIX;
        //获取登录状态,是否登录
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getId() != null){
            key += userInfo.getId();
        }else {
            key += userInfo.getUserKey();
        }
        return key;
    }


    @Override
    public List<Cart> queryCarts() {

        //获取登录状态
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //查询未登录的购物车
        String unLoginKey = KEY_PREFIX + userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> cartJsonList = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if(!CollectionUtils.isEmpty(cartJsonList)){
            unLoginCarts = cartJsonList.stream().map(castJson -> {
                Cart cart = JSON.parseObject(castJson.toString(), Cart.class);
                //查询当前价格
                String priceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(priceString));
                return cart;
            }).collect(Collectors.toList());
        }
        //判断登录状态
        //未登录,直接返回
        if(userInfo.getId() == null){
            return unLoginCarts;
        }
        //登录,购物车同步
        String LoginKey = KEY_PREFIX + userInfo.getId();
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(LoginKey);
        //将未登录下的购物车与登录账号原有购物车数据进行同步
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                if(loginHashOps.hasKey(cart.getSkuId().toString())){
                    Integer count = cart.getCount();
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount() + count);
                }
                loginHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
            //同步完成后,删除未登录状态下的购物车
            this.redisTemplate.delete(unLoginKey);
        }

        //查询登录状态下的购物车
        List<Object> loginCartJsonList = loginHashOps.values();
        List<Cart> carts = loginCartJsonList.stream().map(cartJson -> {
            Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
            //查询当前价格
            String priceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
            cart.setCurrentPrice(new BigDecimal(priceString));
            return cart;
        }).collect(Collectors.toList());

        return carts;
    }

    @Override
    public void updateCart(Cart cart) {

        //获取登录状态
        String key = this.getLoginState();

        //获取购物车
        BoundHashOperations<String, Object, Object> boundHashOps = this.redisTemplate.boundHashOps(key);

        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        //判断更新的这条数据在购物车中有没有
        if(boundHashOps.hasKey(skuId)){
            String cartJson = boundHashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            boundHashOps.put(skuId,JSON.toJSONString(cart));
        }

    }

    @Override
    public void deleteCart(Long skuId) {

        String key = this.getLoginState();
        BoundHashOperations<String, Object, Object> boundHashOps = this.redisTemplate.boundHashOps(key);
        if(boundHashOps.hasKey(skuId.toString())){
            boundHashOps.delete(skuId.toString());
        }
    }
}
