#####黑马点评-后端学习项目
应用：社交类项目         eg：电商、大众点评

Redis就是一个工具，学习时不用太深究里面难的技术（很难），重点是知道Redis能解决什么问题：怎么使用Redis来加快数据传输、处理并发问题

#####  第一阶段：项目基础 &环境搭建    

对应视频：

- 项目介绍
- 环境搭建（MySQL、Redis）
- 前端页面运行
- 后端基础结构

作用：让项目跑起来
 是否重点：❌（略看即可）

------

##### 第二阶段：用户登录模块（Redis入门） 

对应视频：

- 短信验证码登录
- Session → Redis 改造
- 拦截器实现登录校验

 核心知识：

- Redis String
- Token 登录方案

 重要性：⭐⭐⭐（必学）

------

##### 第三阶段：商铺查询（缓存核心）  

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

##### 第四阶段：附近商户（Redis GEO）

对应视频：

- GEO 数据结构
- 附近店铺搜索

重要性：⭐⭐⭐（加分项）

------

##### 第五阶段：点赞 / 排行榜（ZSet）

对应视频：

- 点赞功能
- 排行榜实现

核心：

- ZSet 排序

重要性：⭐⭐⭐

------

##### 第六阶段：签到（Bitmap）

对应视频：

- 用户签到
- 连续签到统计

核心：

- Bitmap 位运算

重要性：⭐⭐（了解即可）

------

##### 第七阶段：秒杀系统（最重要之一）

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

##### 第八阶段：分布式锁

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

​		优点：

![image-20260508104010767](C:\Users\20914\AppData\Roaming\Typora\typora-user-images\image-20260508104010767.png)

​			可重入锁原理：同一线程已持有锁时，再次加锁不用阻塞，直接成功；释放时次数递减，减到 0 才真正删锁
