package com.leyou.service;

import com.leyou.auth.common.pojo.UserInfo;
import com.leyou.auth.common.utils.JwtUtils;
import com.leyou.client.GoodsClient;
import com.leyou.common.utils.JsonUtils;
import com.leyou.interceptor.LoginInterceptor;
import com.leyou.item.pojo.Sku;
import com.leyou.pojo.Cart;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public static final String LOCAL_CART_PREFIX ="leyou:cart:";
    public void saveCart(Cart cart) {
        UserInfo userInfo = LoginInterceptor.get();
        String key =LOCAL_CART_PREFIX+userInfo.getId();
        Integer num = cart.getNum();
        //获取hash对象
        BoundHashOperations<String, Object, Object> hashOperations = stringRedisTemplate.boundHashOps(key);
        Boolean aBoolean = hashOperations.hasKey(cart.getSkuId().toString());
        if(aBoolean){
            //如果存在，改变数量并保存
            String cartStr = hashOperations.get(cart.getSkuId().toString()).toString();
            //反序列化
            cart = JsonUtils.parse(cartStr,Cart.class);
            cart.setNum(cart.getNum()+num);
            //重新保存，添加到redis中
            hashOperations.put(cart.getSkuId().toString(),JsonUtils.serialize(cart));
        }else{
            cart.setUserId(userInfo.getId());
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setTitle(sku.getTitle());
            cart.setPrice(sku.getPrice());
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setImage(StringUtils.isBlank(sku.getImages())?"":StringUtils.split(sku.getImages(),",")[0]);
            //添加到redis中
            hashOperations.put(cart.getSkuId().toString(),JsonUtils.serialize(cart));
        }
    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginInterceptor.get();
        String key =LOCAL_CART_PREFIX+userInfo.getId();
        //获取hash对象
        BoundHashOperations<String, Object, Object> hashOperations = stringRedisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOperations.values();
        return cartJsons.stream().map(cartJson->JsonUtils.parse(cartJson.toString(),Cart.class)).collect(Collectors.toList());
    }
}
