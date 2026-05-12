# 趣享社压力测试套件

## 目录结构

```
test/performance/
├── quxiangshe-test-plan.jmx    # JMeter 测试计划
├── load-test-data.sql           # 测试数据准备SQL
├── run-test.bat                 # Windows 执行脚本
├── run-test.sh                  # Linux/Mac 执行脚本
└── README.md                    # 本文件
```

## 快速开始

### 1. 环境要求

- **JMeter 5.6+**: [下载](https://jmeter.apache.org/download_jmeter.cgi)
- **Docker & Docker Compose**: 用于启动被测服务
- **数据库**: MySQL 8.0+

### 2. 配置

编辑 `run-test.bat` 或 `run-test.sh`，修改 JMeter 路径：

```bash
# Windows
set JMETER_HOME=D:\apache-jmeter-5.6.3

# Linux/Mac
export JMETER_HOME=/opt/apache-jmeter-5.6.3
```

### 3. 准备测试数据

```bash
# 登录 MySQL
mysql -u root -p quxiangshe < load-test-data.sql
```

或在 Docker 环境中：

```bash
docker exec -i lixiang-mysql mysql -u root -p123456 quxiangshe < load-test-data.sql
```

### 4. 执行压测

**Windows:**
```bash
run-test.bat
```

**Linux/Mac:**
```bash
chmod +x run-test.sh
./run-test.sh
```

## 测试场景

| 场景 | 占比 | API | 说明 |
|------|------|-----|------|
| Feed读取 | 40% | GET /api/feed | 缓存命中读 |
| 热点笔记 | 20% | GET /api/note/popular | 热榜读取 |
| 点赞 | 15% | POST /api/note/{id}/like | 写操作 |
| 评论 | 10% | POST /api/note/comment | 写操作 |
| 关注 | 10% | POST /api/follow/{userId} | 写操作 |
| 通知 | 5% | GET /api/notification/list | 读操作 |

## 压测阶段

| 阶段 | 线程数 | 时长 | 目标 |
|------|--------|------|------|
| 预热 | 100 | 5min | 验证服务正常 |
| Phase 1 | 200 | 5min | 基准测试 |
| Phase 2 | 500 | 5min | 递增压测 |
| Phase 3 | 1000 | 5min | 峰值测试 |

## 结果分析

### 输出文件

| 文件 | 说明 |
|------|------|
| `results/phase*-report/` | HTML 报告目录 |
| `results/phase*.csv` | 详细 CSV 结果 |
| `results/summary-all.csv` | 汇总数据 |

### 关键指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| P99 延迟 | < 100ms | Feed 接口 |
| QPS | > 1000 | 系统吞吐量 |
| 错误率 | < 1% | 请求成功率 |

### 查看 HTML 报告

```bash
# 打开 Phase 1 报告
open results/phase1-report/index.html
```

## 自定义压测

### 修改线程数

编辑 JMX 文件或在命令行覆盖：

```bash
jmeter -n -t quxiangshe-test-plan.jmx -Jthread.num_threads=500
```

### 修改测试时长

```bash
jmeter -n -t quxiangshe-test-plan.jmx -Jthread.duration=600
```

### 添加新 API

在 JMX 文件中添加 `HTTPSamplerProxy` 节点：

```xml
<HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET /new/api">
  <stringProp name="HTTPSampler.path">/api/new/api</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
</HTTPSamplerProxy>
```

## 常见问题

### 1. JMeter 内存不足

修改 `jmeter.bat` 或 `jmeter` 脚本：

```bash
HEAP="-Xms4g -Xmx8g"
```

### 2. 连接被拒绝

确保后端服务已启动：
```bash
curl http://localhost:8080/api/auth/health
```

### 3. 数据准备失败

检查 MySQL 连接：
```bash
mysql -h localhost -P 3307 -u root -p
```

## 压测指标参考

### Feed 接口

| 并发 | P50 | P90 | P99 | QPS |
|------|-----|-----|-----|-----|
| 100 | <10ms | <20ms | <50ms | >500 |
| 500 | <20ms | <50ms | <100ms | >2000 |
| 1000 | <50ms | <100ms | <200ms | >3000 |

### 热点笔记接口

| 并发 | P50 | P90 | P99 | QPS |
|------|-----|-----|-----|-----|
| 100 | <5ms | <10ms | <20ms | >1000 |
| 500 | <10ms | <30ms | <50ms | >3000 |
| 1000 | <20ms | <50ms | <100ms | >5000 |

## 联系

如有问题，请提交 Issue 或联系开发者。
