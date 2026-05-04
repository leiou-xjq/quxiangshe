# Canal Server 部署详细指南

## 一、概述

Canal是阿里巴巴开源的**数据库 binlog 日志同步工具**，通过解析MySQL的binlog日志，实现数据的增量订阅和同步。

### 1.1 工作原理

```
┌─────────────┐    binlog     ┌─────────────┐    Canal     ┌─────────────┐
│    MySQL    │ ────────────► │ Canal Server│ ───────────► │  App Client │
│   (主库)    │               │             │   Protocol  │ (消费者)   │
└─────────────┘               └─────────────┘              └─────────────┘
```

### 1.2 架构组件

| 组件 | 说明 |
|------|------|
| Canal Server | 运行在独立服务器，负责连接MySQL并解析binlog |
| Canal Instance | Canal Server中的实例，对应一个MySQL实例 |
| Canal Client | 应用端客户端，连接Server获取数据 |
| Zookeeper | (可选) Canal集群模式下用于服务发现 |

---

## 二、环境要求

### 2.1 硬件要求

| 资源 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2核 | 4核 |
| 内存 | 4GB | 8GB |
| 磁盘 | 20GB | 50GB SSD |
| 网络 | 100Mbps | 1Gbps |

### 2.2 软件要求

| 软件 | 版本要求 |
|------|----------|
| JDK | JDK 8 或 JDK 11 |
| MySQL | 5.7.x 或 8.0.x |
| 操作系统 | Linux (CentOS 7+/Ubuntu 18.04+) |

---

## 三、MySQL配置

### 3.1 开启binlog

编辑 MySQL 配置文件 `my.cnf`：

```ini
[mysqld]
# 开启binlog
log-bin=mysql-bin
# binlog格式
binlog-format=ROW
# binlog过期时间（天）
expire-logs-days=7
# server-id（必须唯一）
server-id=1
# binlog日志目录
log-bin=/var/lib/mysql/mysql-bin
```

### 3.2 创建Canal专用用户

```sql
-- 创建canal用户
CREATE USER 'canal'@'%' IDENTIFIED BY 'canal123';

-- 授予权限
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

### 3.3 验证binlog状态

```sql
-- 查看binlog是否开启
SHOW VARIABLES LIKE 'log_bin';

-- 查看binlog格式
SHOW VARIABLES LIKE 'binlog_format';

-- 查看binlog文件列表
SHOW BINARY LOGS;
```

---

## 四、Canal Server部署

### 4.1 下载Canal

```bash
# 创建安装目录
mkdir -p /opt/canal
cd /opt/canal

# 下载Canal Server (1.1.7版本)
wget https://github.com/alibaba/canal/releases/download/canal-1.1.7/canal.deployer-1.1.7.tar.gz

# 解压
tar -xzf canal.deployer-1.1.7.tar.gz
```

### 4.2 目录结构

```
canal/
├── bin/                    # 启动脚本
│   ├── startup.sh
│   └── stop.sh
├── conf/                   # 配置文件
│   ├── canal.properties    # 主配置
│   └── example/            # 实例配置
│       └── instance.properties
├── logs/                   # 日志目录
│   └── canal/
└── lib/                    # 依赖库
```

### 4.3 配置Canal Instance

编辑 `conf/example/instance.properties`：

```properties
# =====================================================
# MySQL配置
# =====================================================
canal.instance.master.address=127.0.0.1:3306

# mysql主库链接时起始的binlog文件
canal.instance.master.journal.name=

# mysql主库链接时起始的binlog偏移量
canal.instance.master.position=

# mysql主库链接时起始的binlog时间戳
canal.instance.master.timestamp=

# mysql主库链接时的用户名和密码
canal.instance.dbUsername=canal
canal.instance.dbPassword=canal123

# 默认数据库名称
canal.instance.defaultDatabaseName=quxiangshe

# =====================================================
# 表过滤配置
# =====================================================
# 过滤监听的表（正则表达式，多个用逗号分隔）
# .*\\..* 表示监听所有表
# quxiangshe\\.t_note 表示只监听quxiangshe库的t_note表
canal.instance.filter.regex=quxiangshe\\.t_note,quxiangshe\\.user

# 排除的表（正则表达式）
canal.instance.filter.black.regex=

# =====================================================
# 消费模式配置
# =====================================================
# tcp, kafka, rocketMQ
canal.mq.topic=quxiangshe

# 区分instance的topic
canal.mq.dynamicTopic=quxiangshe\\..*
canal.mq.partitionNum=3

# =====================================================
# 其他配置
# =====================================================
# 批量获取binlog的条数
canal.instance.batch.size=5

# 网络超时时间（毫秒）
canal.instance.networkTimeout=30000
```

### 4.4 配置Canal主文件

编辑 `conf/canal.properties`：

```properties
# =====================================================
# Canal主配置
# =====================================================
# canal server端口
canal.port=11111

# canal server admin端口
canal.adminPort=9100

# zookeeper地址（集群模式需要）
# canal.zookeeperServers=127.0.0.1:2181

# 实例列表（逗号分隔）
canal.instance.global.mode=standalone
canal.instance.global.spring.xml=classpath:spring/default-instance.xml

# =====================================================
# 队列配置
# =====================================================
# 使用内存队列（单机模式）
canal.queue.mem.size=10000
canal.queue.mem.batchMarkup=100

# 使用文件队列（持久化模式）
# canal.queue.file.size=10000
# canal.queue.file.flushMarkup=100
```

---

## 五、启动与验证

### 5.1 启动Canal

```bash
# 进入canal目录
cd /opt/canal

# 启动
./bin/startup.sh

# 查看日志
tail -f logs/canal/canal.log

# 查看instance日志
tail -f logs/canal/example/example.log
```

### 5.2 验证启动状态

```bash
# 检查进程
ps -ef | grep canal

# 检查端口
netstat -tlnp | grep 11111

# 查看Canal状态（通过admin端口）
curl http://127.0.0.1:9100/api/v1/instances
```

### 5.3 常见问题排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 无法连接MySQL | binlog未开启或用户权限不足 | 检查MySQL配置和用户权限 |
| 无数据变化 | filter.regex配置错误 | 检查表过滤配置 |
| 内存占用高 | batch.size过大 | 调小批量大小 |

---

## 六、应用端配置

### 6.1 开启Canal同步

在应用的 `application.yml` 中配置：

```yaml
canal:
  enabled: true
  server: 127.0.0.1:11111
  destination: quxiangshe
  username: canal
  password: canal123
  tables: t_note,user
  batch-size: 5
  threads: 2
```

### 6.2 测试同步

1. **插入数据测试**
```sql
INSERT INTO t_note (user_id, title, content, status, audit_status, deleted) 
VALUES (1, '测试笔记', '这是测试内容', 1, 1, 0);
```

2. **查看ES索引**
```bash
curl 'http://localhost:9200/t_note/_search?q=测试'
```

3. **更新数据测试**
```sql
UPDATE t_note SET title = '更新后的标题' WHERE id = 1;
```

4. **删除数据测试**
```sql
DELETE FROM t_note WHERE id = 1;
```

---

## 七、集群部署（可选）

### 7.1 架构

```
                    ┌─────────────┐
                    │  Zookeeper │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
    │Canal 1  │      │Canal 2  │      │Canal 3  │
    └────┬────┘      └────┬────┘      └────┬────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           │
                    ┌──────▼──────┐
                    │  App Client  │
                    │ (负载均衡)   │
                    └─────────────┘
```

### 7.2 Zookeeper配置

```bash
# 安装Zookeeper
wget https://archive.apache.org/dist/zookeeper/zookeeper-3.7.1/apache-zookeeper-3.7.1-bin.tar.gz
tar -xzf apache-zookeeper-3.7.1-bin.tar.gz

# 配置zoo.cfg
tickTime=2000
dataDir=/opt/zookeeper/data
clientPort=2181
```

### 7.3 Canal集群配置

修改 `conf/canal.properties`：

```properties
# 使用集群模式
canal.instance.global.mode=cluster
canal.instance.global.zookeeperServers=127.0.0.1:2181
canal.instance.global.clusterName=canal-default
```

---

## 八、性能调优

### 8.1 参数调优

| 参数 | 默认值 | 推荐值 | 说明 |
|------|--------|--------|------|
| canal.instance.batch.size | 1 | 5-10 | 批量获取条数 |
| canal.instance.networkTimeout | 30000 | 60000 | 网络超时 |
| canal.mq.partitionNum | 3 | 6-9 | Kafka分区数 |

### 8.2 监控指标

```bash
# 查看消费延迟
curl 'http://localhost:9100/api/v1/instances/quxiangshe/metrics'

# 查看处理条数
tail -f logs/canal/example/example.log | grep "binlog"
```

---

## 九、快速部署脚本

```bash
#!/bin/bash
# canal-deploy.sh

# 安装目录
CANAL_HOME="/opt/canal"
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3306"
MYSQL_USER="canal"
MYSQL_PASS="canal123"

# 创建目录
mkdir -p $CANAL_HOME

# 下载
cd $CANAL_HOME
wget -q https://github.com/alibaba/canal/releases/download/canal-1.1.7/canal.deployer-1.1.7.tar.gz

# 解压
tar -xzf canal.deployer-1.1.7.tar.gz

# 配置instance.properties
cat > $CANAL_HOME/conf/example/instance.properties << EOF
canal.instance.master.address=$MYSQL_HOST:$MYSQL_PORT
canal.instance.dbUsername=$MYSQL_USER
canal.instance.dbPassword=$MYSQL_PASS
canal.instance.filter.regex=quxiangshe\\.t_note,quxiangshe\\.user
canal.mq.topic=quxiangshe
EOF

# 启动
$CANAL_HOME/bin/startup.sh

echo "Canal启动完成，请检查日志: $CANAL_HOME/logs/canal/canal.log"
```

---

## 十、相关文档

| 文档 | 说明 |
|------|------|
| [Canal GitHub](https://github.com/alibaba/canal) | 官方GitHub仓库 |
| [Canal Wiki](https://github.com/alibaba/canal/wiki) | 官方文档 |
| [SEARCH_API.md](../docs/SEARCH_API.md) | 搜索接口文档 |

---

*文档更新时间：2026-04-03*
