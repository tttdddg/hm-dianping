### 黑马点评（基础篇+实战篇）-后端学习项目
应用：社交类项目         eg：电商、大众点评

Redis就是一个工具，学习时不用太深究里面难的技术（很难）

，重点是知道Redis能解决什么问题：怎么使用Redis来加快数据传输、处理并发问题



必看（核心）

- 登录模块（Redis token）
- 商铺缓存（三大问题）
- 秒杀系统（Lua + 一人一单）

次重点

- 分布式锁
- GEO / ZSet（二选一）

可跳

- 前端
- 基础 CRUD
- Bitmap

#####  第一阶段：项目基础 &环境搭建    √

对应视频：

- 项目介绍
- 环境搭建（MySQL、Redis）
- 前端页面运行
- 后端基础结构

作用：让项目跑起来
 是否重点：❌（略看即可）

------

##### 第二阶段：用户登录模块（Redis入门）**√**

对应视频：

- 短信验证码登录
- Session → Redis 改造
- 拦截器实现登录校验

 核心知识：

- Redis String
- Token 登录方案

 重要性：⭐⭐⭐（必学）

------

##### 第三阶段：商铺查询（缓存核心）  √

对应视频：

- 商铺信息查询
- 缓存穿透问题
- 缓存击穿问题
- 缓存雪崩问题
- 逻辑过期 vs 互斥锁

核心：
 这是整个项目最重要的一部分 

重要性：⭐⭐⭐⭐⭐（必须吃透）

------

##### 第四阶段：附近商户（Redis GEO）  √

对应视频：

- GEO 数据结构
- 附近店铺搜索

重要性：⭐⭐⭐（加分项）

------

##### 第五阶段：点赞 / 排行榜（ZSet）  √

对应视频：

- 点赞功能
- 排行榜实现

核心：

- ZSet 排序

重要性：⭐⭐⭐

------

##### 第六阶段：签到（Bitmap）  √

对应视频：

- 用户签到
- 连续签到统计

核心：

- Bitmap 位运算

重要性：⭐⭐（了解即可）

------

##### 第七阶段：秒杀系统（最重要之一）  √

对应视频：

- 秒杀业务介绍
- 一人一单
- 超卖问题
- Redis + Lua 实现原子操作
- 异步下单（Stream / MQ）

核心：
**高并发设计的精华**

**教会你怎么处理很多个并发请求，保证数据库压力不会太大、即使很多请求也能顺利完成任务。**

重要性：⭐⭐⭐⭐⭐（必须会讲）

------

##### 第八阶段：分布式锁   √

对应视频：

- setnx 实现锁
- Redisson（有些版本会讲）

重要性：⭐⭐⭐⭐



导入项目原型



##### 1.短信登录功能：

​		基于session实现登录、登录校验拦截器

（集群session共享问题：多台tomcat不共享存储空间，切换不同tomcat服务会导致数据丢失

--> 替代方案应：数据共享+内存存储+k、v结构 --> redis √）redis替代session

——>基于redis实现共享session登录、状态刷新问题（登录拦截器+token刷新拦截器）

![image-20260410091923072](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260410091923072.png)

![image-20260410092226555](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260410092226555.png)



##### 2.商户查询缓存功能：

​	缓存（cache）：数据交换缓冲区，存储数据临时地方，读写性能较高

![image-20260412233238703](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260412233238703.png)

​	添加商品缓存

​	缓存更新策略：

![image-20260413194441312](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260413194441312.png)

​	eg: 低一致性-->内存淘汰     店铺类型的查询缓存

​		  高一致性-->主动更新（超时剔除作兜底）   店铺详情查询缓存

​	![image-20260413201031377](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260413201031377.png)

​	实现商铺缓存和数据库双写一致

​	

​	缓存穿透：客户端所请求数据在缓存和数据库中都不存在-->缓存永远不会生效-->请求打到数据库

​						解决方案：1.缓存空对象  2.布隆过滤  

​										    3.增加id复杂度   4.基础格式校验  5.用户权限校验  6.热点参数限流

![image-20260414152958465](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260414152958465.png)

​	缓存雪崩：同一时段大量缓存key同时失效 / redis服务宕机  --> 大量请求到达数据库 --> 大压力

​						![image-20260414160452227](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260414160452227.png)

​	缓存击穿（热点Key问题）：当一个被高并发访问并且缓存重建业务较复杂的key突然失效

​													   -->  无数请求访问瞬间给数据库带来大冲击

![image-20260414170429686](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260414170429686.png)

​	缓存工具封装：
​			把stringRedisTemplate封装成类：不确定的时候善于运用泛型



**3.优惠劵秒杀：**

数据库自增id -->  问题：规律性明显+单表数据量限制  

​	--> 全局id生成器（分布式系统下唯一id）：唯一性、递增性、安全性、高可用、高性能 

- 全局唯一id生成策略：UUID、Redis自增、snowflake算法、数据库自增
- Redis自增id策略：每天一个key，方便统计订单量

![image-20260428192206916](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260428192206916.png)

![image-20260429122125814](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260429122125814.png)

​	添加优惠券实现秒杀下单：秒杀是否开始？库存是否充足？

​	库存**超卖**问题：**高并发**   **多线程安全问题**

​			解决：加锁--> 悲观锁：添加同步锁，让线程串行执行    性能一般

​									乐观锁：不加锁，在更新时判断之前查询得到的数据是否有被修改过   性能好但成功率低

​													--> 版本号法

​													     CAS法（库存代替版本）

![image-20260506134748594](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260506134748594.png)

​	加锁可以解决单机情况下的一人一单安全问题，但集群模式下就不行了（synchronized只在单个jvm起作用）

​	分布式锁：满足分布式系统或集群模式下多进程可见且互斥的锁  (互斥：确保只有一个锁能获取线程)

​	实现：

![image-20260507115552732](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260507115552732.png)

​	基于redis的分布式锁实现：获取锁：setnx lock thread1 （超时释放-->添加锁过期时间：expire lock 10)

​																   set lock thread1 nx ex 10  (nx互斥，ex是设置超时时间)

​												   释放锁（手动/超时释放）：del key   

​																								   误删问题（so应先获取线程标示）

​	原子性问题

​	![image-20260507120809496](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260507120809496.png)

​	Lua脚本：在一个脚本中编写多条redis命令，确保多条命令执行时的原子性

​					  eval  “return 脚本”  脚本需要key类型参数个数

​	![image-20260508092403663](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260508092403663.png)

​	lua脚本：

![image-20260508092846863](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260508092846863.png)

​	

​	Redisson：

![image-20260508104010767](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260508104010767.png)

​			优点：

​					可重入锁原理：利用hash结构记录线程id和重入次数，同一线程已持有锁时，再次加锁不用阻塞，直接成功；释放时次数递减，减到 0 才真正删锁

​					可重试：利用信号量和PubSub功能实现等待、唤醒，获取锁失败的重试机制

​					超时续约：利用watchDog,每隔一段时间(releaseTime/3),重置超时时间

​					主从一致性：利用multiLock,多个独立的Redis节点，必须在所有节点都获取重入锁，才算获取锁成功

​		秒杀业务优化：

​						异步优化：基于redis完成秒杀资格判断（库存余量、一人一单）

​										   基于阻塞队列实现秒杀异步优化（放入阻塞队列，独立线程异步下单）

​					    -->内存安全问题、数据限制问题

![image-20260511214258215](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260511214258215.png)

![image-20260511215155926](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260511215155926.png)

​		消息队列：

![image-20260512160641493](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260512160641493.png)

​			Redis提供了三种不同的方式来实现消息队列：

![image-20260513185447631](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260513185447631.png)

​					list结构：基于List结构（双向链表）模拟消息队列

​									 LPUSH+RPOP / RPUSH+LPOP

​									 xPOP：无消息时会返回null--> BxPOP来阻塞

![image-20260512161411233](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260512161411233.png)

​					PubSub（发布订阅）：基本的点对点消息模型

![image-20260512161630678](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260512161630678.png)

![image-20260512162058877](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260512162058877.png)					

​						Stream：比较完善的消息队列模型 √

​										单消息模式：读取消息方式：XREAD

![image-20260512163210994](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260512163210994.png)

​										 消费者组模式：把多个消费者划分到一个组中监听同一个队列

​								  （Consumer Group）

![image-20260513185352099](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260513185352099.png)



##### 4.达人探店：

- 发布笔记（存储照片）
- 查看笔记
- 点赞功能（用set集合存储已经点赞的用户ID，新增IsLike字段标识是否点赞）
- 点赞排行榜功能（改用SortedSet存储用户ID，score值设置为点赞时间，最后根据点赞时间排序）



##### 5.好友关注：

- 关注和取关：有个tb_follow表，存储关注和被关注的人的id
- 共同关注：在更新数据库的同时，将用户关注的id记录到Redis的Set集合中；新增查询目标用户的共同关注接口，求两个用户的Set集合的交集，即为共同关注
- 关注推送：Feed流



##### 6.附近商铺：

- 存储方案设计：按照商户类型做分组，类型相同的商户作为同一组，以typeld为key存入同一个GEO集合中即可（在店铺新增存人的时候，就提前用key分好组）

​	查询店铺的时候看需不需要按距离排序，需要的话就调用GEO来计算，不需要就直接呈现所有店铺



##### 7.用户签到：

​	Redis将BitMap的所有操作封装到字符串String中了，因此spring-data-redis使用opsForValue,操作BitMap

```Plain
stringRedisTemplate_opsForValue（).setBit key, dayOfMonth -1, true)1 stringRedisTemplate.opsForValue().setBit(key,dayofMonth-1,true);
```

- key:哪个用户
- dayOfMonth:哪一天
- true:签到



##### 8.UV统计：

UV：全称Unique visitor，也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人。1天内同一个用户多次UV:全称Unique Visitor,也叫独立访客量，是指通过互联网访问、浏览这个网页的自然人。1天内同一个用户多次访问该网站，只记录1次。

PV：全称PageView，也叫页面访问量或点击量，用户每访问网站的一个页面，记录1次PV，用户多次打开页面，PV:全称Page View,也叫页面访问量或点击量，用户每访问网站的一个页面，记录1次PV,用户多次打开页面，则记录多次PV。往往用来衡量网站的流量。

```java
@Test
void testHyperLogLog() {
    String[] values = new String[1000];
    for (int i = 0; i < 1000000; i++) {
        values[i % 1000] = "user_" + i;
        // 每1000次，添加一次，添加到Redis
        if (i % 1000 == 999) {
            stringRedisTemplate.opsForHyperLogLog().add("hl2", values);
        }
    }
    // 统计数量
    Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
    System.out.println("count = " + count); // count = 997593
}
```

