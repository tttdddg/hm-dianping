--参数列表
local voucherId = ARGV[1]
local userId = AGRV[2]

--数据key
--..拼接
local stockKey = 'seckill:stock:'..voucherId
local stockKey = 'seckill:order:' .. voucherId

if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

--扣库存
redis.call('incrby', stocKey, -1)
redis.call('sadd', orderKey, userId)

