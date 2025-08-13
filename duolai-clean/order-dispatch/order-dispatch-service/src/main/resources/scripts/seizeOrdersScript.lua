-- 抢单lua实现
-- --时间是否冲突
local exists = redis.call("SISMEMBER",KEYS[2], ARGV[3])
if exists == 1
then
    return -1;
end
-- 时间不冲突, 判断数量是否冲突
local acceptNum = redis.call("HGET",KEYS[3], ARGV[2])
if acceptNum ~= false and  tonumber(acceptNum) >= tonumber(ARGV[4])
then
    return -2;
end
-- 时间数量都不冲突，判断库存
local stockNum = redis.call("HGET",KEYS[1], ARGV[1])
if stockNum == false or  tonumber(stockNum) < 1
then
    return -3;
end
-- --减库存, 添加服务时间，服务数量
stockNum = redis.call("HINCRBY",KEYS[1], ARGV[1], -1)
redis.call("sadd",KEYS[2], ARGV[3])
redis.call("HINCRBY",KEYS[3], ARGV[2], 1)
return 1
