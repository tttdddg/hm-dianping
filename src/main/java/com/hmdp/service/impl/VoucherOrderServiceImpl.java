package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import io.lettuce.core.RedisClient;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Autowired
    private IVoucherOrderService iVoucherOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Override
//    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //查询秒杀优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        //判断秒杀是否开始/结束
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }

        //判断秒杀优惠券库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        //先获取锁再进入创建订单函数
        Long userId = UserHolder.getUser().getId();
//        SimpleRedisLock lock=new SimpleRedisLock("order:"+userId,stringRedisTemplate);
        RLock lock=redissonClient.getLock("lock:order:"+userId);
        boolean isLock=lock.tryLock();
        if(!isLock){
            return Result.fail("不允许重复下单");
        }
        try{
            //启动层设为暴露-->能获取代理对象（事务）
            IVoucherOrderService proxy=(IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally{
            lock.unlock();
        }
//        synchronized(userId.toString().intern()){
//            //启动层设为暴露-->能获取代理对象（事务）
//            IVoucherOrderService proxy=(IVoucherOrderService)AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        //一人一单
        Long userId = UserHolder.getUser().getId();
        //1.查询订单
        int count=query().eq("user_id",userId).eq("voucher_id",voucherId).count();
        //2.判断是否存在
        if(count>0){
            return Result.fail("用户已经购买过一次");
        }
        //扣减库存
        boolean success=seckillVoucherService.update()
                .setSql("stock=stock-1") //set stock=stock-1
//                .eq("voucher_id",voucherId).eq("stock",voucher.getStock()) //where id=? and stock=?
                .eq("voucher_id",voucherId).gt("stock",0) ////where id=? and stock>0
                .update();
        if(!success){
            //扣减失败
            return Result.fail("库存不足");
        }
        //生成订单
        VoucherOrder voucherOrder=new VoucherOrder();
        //1.订单id
        long orderId=redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //2.用户id
        voucherOrder.setUserId(userId);
        //3.代金券id
        voucherOrder.setVoucherId(voucherId);
        iVoucherOrderService.save(voucherOrder);

        //返回订单id
        return Result.ok(orderId);
    }
}
