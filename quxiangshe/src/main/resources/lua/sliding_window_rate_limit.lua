-- 滑动窗口限流 Lua 脚本
-- 功能：原子化执行「删除窗口外数据→查询窗口内请求数→阈值判断→计数自增→设置key过期时间」
-- 参数：KEYS[1] - 限流key, ARGV[1] - 当前时间戳(毫秒), ARGV[2] - 窗口大小(毫秒), ARGV[3] - 阈值, ARGV[4] - key过期时间(毫秒)
-- 返回值：1-允许, 0-拒绝

local key = KEYS[1]
local currentTime = tonumber(ARGV[1])
local windowSize = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local expireTime = tonumber(ARGV[4])

-- 1. 删除窗口外无效计数
local windowStart = currentTime - windowSize
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- 2. 查询窗口内请求数
local count = redis.call('ZCARD', key)

-- 3. 阈值判断
if count >= limit then
    return 0
end

-- 4. 计数自增
local requestId = currentTime .. '-' .. math.random(1000, 9999)
redis.call('ZADD', key, currentTime, requestId)

-- 5. 设置key过期时间（略大于窗口时间，避免内存堆积）
redis.call('PEXPIRE', key, expireTime)

return 1