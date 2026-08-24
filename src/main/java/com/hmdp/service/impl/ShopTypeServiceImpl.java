package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result querySort() {
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range(RedisConstants.SHOP_TYPE_KEY, 0, -1);
        if (shopTypeJson != null && !shopTypeJson.isEmpty()) {
            List<ShopType> shopTypes = shopTypeJson.stream()
                    .map(shopType -> JSONUtil.toBean(shopType, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes==null||shopTypes.isEmpty()){
            return Result.fail("没有分类数据");
        }
        for (ShopType shopType : shopTypes) {
            String json = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().rightPush(RedisConstants.SHOP_TYPE_KEY,json);
        }
        return Result.ok(shopTypes);
        //1.先从redis缓存看是否存在
        // String shopTypeJson = stringRedisTemplate.opsForValue().get(RedisConstants.SHOP_TYPE_KEY);
//        if(StrUtil.isNotBlank(shopTypeJson)) {
//            //2.如果存在，返回分类信息
//            return Result.ok(JSONUtil.toList(shopTypeJson, ShopType.class));
//        }
//      //3.不存在，则查询数据库
//        List<ShopType> shopTypes = query().orderByAsc("sort").list();
//        if(shopTypes==null) {
//            //4.不存在，则返回错误信息
//            return Result.fail("没有分类数据");
//        }
//        // 5.数据库存在在，则存入redis，返回分类信息
//        String json = StrUtil.toString(shopTypes);
//        stringRedisTemplate.opsForValue().set(RedisConstants.SHOP_TYPE_KEY,json);
//
//        return Result.ok(shopTypes);
    }
}
