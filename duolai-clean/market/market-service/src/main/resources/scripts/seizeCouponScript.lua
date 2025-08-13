-- 抢券lua实现
-- key: 优惠卷库存，抢券成功列表
-- argv：活动id,用户id

--优惠券是否已经抢过
-- KEYS[2]: 某一个用户的已抢优惠卷活动id集合的key
-- ARGV[1]: 当前用户正在抢卷的优惠卷活动id
local exists = redis.call("SISMEMBER", KEYS[2], ARGV[1])
-- local couponNum = redis.call("HGET", KEYS[2], ARGV[2])
-- hget 获取不到数据返回false而不是nil
--if couponNum ~= false and tonumber(couponNum) >= 1
if exists == 1
then
    return -1;
end
-- --库存是否充足校验
-- KEYS[1]代表存储所有优惠卷活动库存的hash数据结构值的key
-- ARGV[1] 优惠卷活动id
local stockNum = redis.call("HGET",KEYS[1], ARGV[1])
if stockNum == false or  tonumber(stockNum) < 1
then
    return -2;
end
-- 库存减1
redis.call("HINCRBY",KEYS[1], ARGV[1], -1)
--添加抢券列表
redis.call("SADD",KEYS[2], ARGV[1])
return 1;
