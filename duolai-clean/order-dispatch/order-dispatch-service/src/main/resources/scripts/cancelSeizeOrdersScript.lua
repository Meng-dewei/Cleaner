-- 取消抢单
-- 如果没库存，说明已经被抢单或者派单
local stockNum = redis.call("HGET",KEYS[1], ARGV[1])
if stockNum == false
then
-- 已经被删除掉了，重复取消
    return -1;
end
-- 不是重复取消，已经被抢单成功了
if tonumber(stockNum) <= 0
then
    return -2;
end
-- 有库存，还没有被抢单或者派单, 则删除库存
redis.call("HDEL",KEYS[1], ARGV[1])
return 1
