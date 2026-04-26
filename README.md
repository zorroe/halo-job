# Halo-Job

Halo-Job 是一个基于 Spring Boot 3 的轻量级分布式任务调度项目，采用 `Admin + Executor` 分离架构：

- `halo-job-admin` 负责任务定义、调度扫描、路由分发、执行日志和执行器注册中心。
- `halo-job-core` 提供执行器接入 SDK、公共协议和执行入口。
- `halo-job-executor` 提供一个可直接复制的示例执行器。

当前主分支对应干净版 `v0.2` 基线，只描述新模型，不兼容旧表结构和旧接口。

## v0.2 基线

`v0.2` 解决的是两个基础问题：

- 任务到底发给哪个执行器
- 任务到底按什么触发模型执行

这一版已经完成的核心能力：

- 执行器按 `executor_group + executor_app` 注册和心跳上报
- 执行器在注册时上报本机 `handler` 元数据
- 调度中心维护 `executor_handler` 注册表
- 任务定义强制绑定 `executor_group + executor_app + executor_handler`
- 任务支持 `CRON / ONCE / FIXED_RATE / FIXED_DELAY`
- 调度时只会路由到“在线且声明支持目标 handler”的执行器
- 保留手动触发、重试、路由策略、阻塞策略和执行日志

## 架构

```text
                +----------------------+
                |   halo-job-admin     |
                |  API / Scheduler /   |
                |  Registry / Router   |
                +----------+-----------+
                           |
                           | HTTP dispatch
                           v
        +------------------+------------------+
        |                                     |
+-------+--------+                    +-------+--------+
| halo-job-exec  |                    | halo-job-exec  |
| Executor A     |                    | Executor B     |
| @HaloJob tasks |                    | @HaloJob tasks |
+-------+--------+                    +-------+--------+
        ^                                     ^
        | register / heartbeat                | register / heartbeat
        +------------------+------------------+
                           |
                           v
                +----------------------+
                | MySQL + Redis        |
                | job / executor / log |
                +----------------------+
```

依赖说明：

- MySQL：任务定义、执行器信息、handler 注册表、执行日志
- Redis：调度扫描互斥锁

## 模块

```text
halo-job
|- halo-job-admin      调度中心
|- halo-job-core       公共协议和执行器 SDK
|- halo-job-executor   示例执行器
|- sql                 初始化脚本
`- pom.xml             Maven 聚合工程
```

## 已实现能力

- 任务增删改查、启停、手动触发
- 调度扫描和下次触发时间回写
- `CRON / ONCE / FIXED_RATE / FIXED_DELAY` 触发模型
- 执行器注册、轻量心跳、在线节点维护
- handler 注册中心
- 路由策略：`ROUND / RANDOM / FIRST / LAST / HASH / SHARDING_BROADCAST`
- 阻塞策略：`QUEUE_WAIT / DISCARD_NEW / COVER_RUNNING`
- 调度失败重试
- 执行日志记录

## 数据模型

### `job_info`

| 字段 | 说明 |
| --- | --- |
| `job_name` | 任务名 |
| `executor_group` | 执行器分组，默认 `default` |
| `executor_app` | 执行器应用名，必填 |
| `executor_handler` | 目标 handler 名，必填 |
| `executor_param` | 执行参数 |
| `job_status` | `0=STOP`，`1=RUN` |
| `trigger_type` | `CRON / ONCE / FIXED_RATE / FIXED_DELAY` |
| `trigger_config` | 触发配置 JSON |
| `next_execute_time` | 下次执行时间，由服务端计算 |
| `owner` | 负责人 |
| `tag` | 标签 |
| `remark` | 备注 |
| `route_strategy` | 路由策略编码 |
| `block_strategy` | 阻塞策略编码 |
| `retry_count` | 调度失败重试次数 |

说明：

- `trigger_type` 和 `trigger_config` 要么同时为空，要么同时存在。
- 两者同时为空时，任务不会自动调度，只能手动触发。
- 保存任务时，admin 会校验 `executor_group + executor_app + executor_handler` 是否存在于注册中心。

### `executor_info`

| 字段 | 说明 |
| --- | --- |
| `executor_name` | 执行器名称 |
| `executor_address` | 执行器地址 |
| `executor_group` | 执行器分组 |
| `executor_app` | 执行器应用名 |
| `metadata` | 扩展元数据 JSON |
| `version` | 执行器版本 |
| `heartbeat_time` | 最近心跳时间 |
| `status` | `0=OFFLINE`，`1=ONLINE` |

### `executor_handler`

| 字段 | 说明 |
| --- | --- |
| `executor_name` | 执行器名称 |
| `executor_address` | 执行器地址 |
| `executor_group` | 执行器分组 |
| `executor_app` | 执行器应用名 |
| `handler_name` | handler 名 |
| `handler_desc` | handler 描述 |
| `method_signature` | 方法签名摘要 |

## 触发模型

| 类型 | `trigger_config` 示例 | 说明 |
| --- | --- | --- |
| `CRON` | `{"cronExpression":"0 */5 * * * ?"}` | Cron 任务 |
| `ONCE` | `{"triggerAt":1760000000000}` | 单次任务，毫秒时间戳 |
| `FIXED_RATE` | `{"intervalSeconds":60,"startAt":1760000000000}` | 固定频率 |
| `FIXED_DELAY` | `{"delaySeconds":60,"startAt":1760000000000}` | 固定延迟 |

## 执行器协议

### 注册

`POST /executor/api/register`

请求体示例：

```json
{
  "name": "halo-job-demo-executor",
  "address": "http://127.0.0.1:9090",
  "group": "default",
  "app": "halo-job-executor",
  "version": "0.0.1-SNAPSHOT",
  "metadata": "{\"zone\":\"local\"}",
  "handlers": [
    {
      "handlerName": "dataSyncTask",
      "description": "demo data sync task",
      "methodSignature": "void dataSync(java.lang.String)"
    }
  ]
}
```

说明：

- 注册请求必须包含 handler 列表。
- 同一个 `group + app + handler` 如果出现不同方法签名，admin 会拒绝注册。

### 心跳

`POST /executor/api/beat`

请求体示例：

```json
{
  "name": "halo-job-demo-executor",
  "address": "http://127.0.0.1:9090",
  "group": "default",
  "app": "halo-job-executor",
  "version": "0.0.1-SNAPSHOT"
}
```

说明：

- 心跳只更新在线状态和最近心跳时间。
- 心跳不再重复同步 handler 列表。

### 调度下发

`POST /executor/run`

请求体字段：

- `jobId`
- `jobName`
- `handler`
- `param`
- `blockStrategy`
- `shardIndex`
- `shardTotal`
- `executorAddress`
- `triggerType`

## 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 5.7+ 或 8.x
- Redis 6.x+

### 2. 初始化数据库

执行：

```bash
mysql -uroot -p < sql/halo-job-init.sql
```

当前只提供干净初始化脚本，不提供旧版本升级脚本。

### 3. 修改配置

把仓库中的本地开发配置替换成你自己的环境参数。

`halo-job-admin/src/main/resources/application.yaml` 参考：

```yaml
server:
  port: 8080

spring:
  application:
    name: halo-job-admin
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://127.0.0.1:3306/halo-job?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your_mysql_user
    password: your_mysql_password
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_redis_password
      database: 0

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.zorroe.cloud.job.admin.entity
```

`halo-job-executor/src/main/resources/application.yaml` 参考：

```yaml
server:
  port: 9090

spring:
  application:
    name: halo-job-executor

executor:
  admin: http://127.0.0.1:8080
  name: halo-job-demo-executor
  group: default
  app: halo-job-executor
  version: 0.0.1-SNAPSHOT
  address: http://127.0.0.1:9090
```

说明：

- `executor.group` 和 `executor.app` 一起决定任务归属。
- `executor.address` 必须是 admin 可访问到的实际地址。

### 4. 构建

```bash
mvn clean package -DskipTests
```

### 5. 启动

推荐顺序：

1. 启动 `HaloJobAdminApplication`
2. 启动 `HaloJobExecutorApplication`

### 6. 验证

查看在线执行器：

```bash
curl http://127.0.0.1:8080/executor/api/onlineList
```

查看任务列表：

```bash
curl http://127.0.0.1:8080/admin/job/list
```

手动触发任务：

```bash
curl "http://127.0.0.1:8080/schedule/trigger?jobId=1"
```

查看执行日志：

```bash
curl http://127.0.0.1:8080/admin/job/log/list
```

## 主要接口

### 任务管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/admin/job/save` | 新增或更新任务 |
| `GET` | `/admin/job/get/{id}` | 任务详情 |
| `GET` | `/admin/job/list` | 任务列表 |
| `GET` | `/admin/job/handlers` | 查询可用 handler |
| `POST` | `/admin/job/changeStatus/{id}/{status}` | 启停任务 |

### 调度与日志

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/schedule/trigger?jobId={id}` | 手动触发 |
| `GET` | `/admin/job/log/list` | 执行日志 |

### 执行器管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/executor/api/register` | 执行器注册 |
| `POST` | `/executor/api/beat` | 轻量心跳 |
| `GET` | `/executor/api/onlineList` | 在线执行器列表 |
| `GET` | `/admin/executor/list` | 按分组、应用、状态查询执行器 |

### 执行器内部接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/executor/run` | 调度中心标准下发入口 |

## 创建任务示例

通过 `POST /admin/job/save` 创建一个固定频率任务：

```json
{
  "jobName": "Demo Data Sync",
  "executorGroup": "default",
  "executorApp": "halo-job-executor",
  "executorHandler": "dataSyncTask",
  "executorParam": "tenant=demo",
  "jobStatus": 1,
  "triggerType": "FIXED_RATE",
  "triggerConfig": "{\"intervalSeconds\":300}",
  "routeStrategy": 1,
  "blockStrategy": 1,
  "retryCount": 1,
  "remark": "every 5 minutes"
}
```

说明：

- `id` 为空时新增，不为空时更新。
- `executorGroup` 默认值是 `default`，但建议显式传入。
- `executorApp` 和 `executorHandler` 必填。
- `nextExecuteTime` 由服务端计算，不需要手工传。

## 执行器接入

如果你要新接一个业务执行器，最简单的方式就是参考 `halo-job-executor` 模块。

### 1. 启用执行器能力

```java
@EnableHaloJobExecutor
@SpringBootApplication
public class OrderExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderExecutorApplication.class, args);
    }
}
```

### 2. 声明任务

```java
@Component
public class OrderJob {

    @HaloJob("orderSyncTask")
    public void sync(String param, HaloJobContext context) {
        System.out.println(
                "param=" + param
                        + ", shard=" + (context.getShardIndex() + 1)
                        + "/" + context.getShardTotal()
        );
    }
}
```

### 3. 支持的方法签名

```java
@HaloJob("jobA")
public void jobA()

@HaloJob("jobB")
public void jobB(String param)

@HaloJob("jobC")
public void jobC(HaloJobContext context)

@HaloJob("jobD")
public void jobD(String param, HaloJobContext context)
```

## 当前边界

`v0.2` 还没有覆盖这些能力：

- Misfire 补偿
- 任务实例和 attempt 模型
- 超时、取消、重跑
- 日志分页查询
- 权限、审计、管理 UI
- DAG 编排和告警

这些内容会进入后续版本，见 [ROADMAP.md](./ROADMAP.md)。
