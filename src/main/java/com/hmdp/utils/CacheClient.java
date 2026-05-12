package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    private StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate=stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallBack,Long time, TimeUnit unit
    ){
        String key=keyPrefix+id;
        //从redis查询商品缓存
        String json=stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isNotBlank(json)){
            //存在-->直接返回
            return JSONUtil.toBean(json,type);
        }
        //判断命中是否是空值
        if(json!=null){
            return null;
        }

        //不存在-->根据id查询数据库
        R r=dbFallBack.apply(id);
        if(r==null){
            //数据不存在-->写入空值
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //存在-->写入Redis
        this.set(key,r,time,unit);

        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type,Function<ID,R> dbFallBack,Long time, TimeUnit unit
    ){
        String key=keyPrefix+id;
        //从redis查询商品缓存
        String json=stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isBlank(json)){
            //存在-->直接返回
            return null;
        }
        //命中-->将json反序列化转为对象
        RedisData redisData=JSONUtil.toBean(json,RedisData.class);
        R r=JSONUtil.toBean((JSONObject)redisData.getData(),type);
        LocalDateTime expireTime=redisData.getExpireTime();

        //判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //1.未过期-->返回店铺信息
            return r;
        }
        //2.过期-->返回逻辑过期

        //实现缓存重建
        //1.获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //2.判断是否获取成功
        if (isLock) {
            //3.获取锁成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //查询数据库
                    R r1=dbFallBack.apply(id);
                    //写入redis
                    this.setWithLogicalExpire(key,r1,time,unit);
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    unlock(lockKey);
                }
            });
        }
        //4.失败-->返回过期店铺信息
        return r;
    }

    public <R,ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type,Function<ID,R> dbFallBack,Long time, TimeUnit unit
    ){
        String key=keyPrefix+id;
        //从redis查询商品缓存
        String json=stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isNotBlank(json)){
            //存在-->返回
            R r= JSONUtil.toBean(json,type);
            return r;
        }
        //判断命中的是否是空值
        if(json!=null){
            return null;
        }
        //实现缓存重建
        //1.获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        R r=null;
        try {

            //2.判断是否获取成功
            if (!isLock) {
                //3.失败并重试
                Thread.sleep(200);
                return queryWithMutex(keyPrefix,id,type,dbFallBack,time,unit);
            }
            //4.获取锁成功，根据id查询数据库
            r=dbFallBack.apply(id);
            //不存在-->返回错误
            if (r == null) {
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);

                return null;
            }
            //存在-->写入redis
            this.set(key,r,time,unit);
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放锁
            unlock(lockKey);
        }
        //返回
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
