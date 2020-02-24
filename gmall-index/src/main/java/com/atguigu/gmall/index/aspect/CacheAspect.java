package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class CacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object aroud(ProceedingJoinPoint joinPoint) throws Throwable{

        Object result = null;

        //获取目标方法
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        //获取注解中的缓存前缀
        String prefix = gmallCache.prefix();
        //获取注解中的过期时间
        int timeout = gmallCache.timeout();
        //获取注解中的随机时间
        int random = gmallCache.random();
        //获取目标方法的返回值
        Class<?> returnType = method.getReturnType();


        //获取目标方法的参数列表
        Object[] args = joinPoint.getArgs();

        //从缓存中查询
        String key = prefix + Arrays.asList(args).toString();
        result = this.cacheHit(key, returnType);
        if (result != null){
            return result;
        }

        //没有命中，加分布式锁
        RLock lock = this.redissonClient.getLock("lock" + Arrays.asList(args).toString());
        lock.lock();

        //再次从缓存中查询,如果没有,执行目标方法(因为如果1000个请求，都到分布式锁锁住这块了，万一第一个完成了，把结果存入缓存，那么接下来的999个请求，需要再判断下缓存有没有，避免999都再次查询mysql，造成击穿)
        result = this.cacheHit(key, returnType);
        if (result != null){
            //释放分布式锁
            lock.unlock();
            return result;
        }

        //执行目标方法
        result = joinPoint.proceed(args);

        //查询结果放入缓存,释放分布式锁
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout + (int) (Math.random() * random), TimeUnit.MINUTES);
        return result;
    }

    private Object cacheHit(String key,Class<?> returnType){

        //从缓存中查询
        String json = this.redisTemplate.opsForValue().get(key);

        //命中，直接返回
        if(StringUtils.isNoneBlank(json)){
            return JSON.parseObject(json,returnType);
        }
        return null;
    }
}
