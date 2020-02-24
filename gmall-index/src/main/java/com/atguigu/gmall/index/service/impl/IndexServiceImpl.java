package com.atguigu.gmall.index.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVO;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsApi gmallPmsApi;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Override
    public List<CategoryEntity> queryLvl1Categories() {

        Resp<List<CategoryEntity>> listResp = this.gmallPmsApi.queryCategoriesByPidOrLevel(1, null);
        return listResp.getData();
    }

    @GmallCache(prefix = "index:cates:", timeout = 7200, random = 100)
    public List<CategoryVO> querySubCategories(Long pid) {

        //1、判断缓存中有没有
        //String cateJson = this.redisTemplate.opsForValue().get(KEY_PREFIX+pid);
        //2、如果有直接返回
        //if(!StringUtils.isEmpty(cateJson)){
        //    List<CategoryVO> categoryVOS = JSON.parseArray(cateJson, CategoryVO.class);
        //    return categoryVOS;
        // }

        //分布式锁锁住
        //RLock lock = this.redissonClient.getLock("lock" + pid);
        //lock.lock();

        //再次判断缓存中有没有,因为如果1000个请求，都到分布式锁锁住这块了，万一第一个完成了，把结果存入缓存，那么接下来的999个请求，需要再判断下缓存有没有，避免999都再次查询mysql，造成击穿
        //1、判断缓存中有没有
        //String cateJson2 = this.redisTemplate.opsForValue().get(KEY_PREFIX+pid);
        //2、如果有直接返回
        //if(!StringUtils.isEmpty(cateJson)){
        //   List<CategoryVO> categoryVOS = JSON.parseArray(cateJson, CategoryVO.class);
        //    lock.unlock();
        //    return categoryVOS;
        //}

        //3、如果没有，查询完成后，放入缓存
        Resp<List<CategoryVO>> listResp = this.gmallPmsApi.querySubCategories(pid);
        List<CategoryVO> categoryVOS = listResp.getData();
        //this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(categoryVOS),7 + new Random().nextInt(5), TimeUnit.DAYS);

        //lock.unlock();

        return listResp.getData();
    }
}
