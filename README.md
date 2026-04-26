# Halo-Job

Halo-Job 是一个基于 Spring Boot 3 的轻量级分布式任务调度项目，采用 `Admin（调度中心）+ Executor（执行器）` 的拆分架构，面向“任务配置集中管理、执行器分布式执行、按策略路由下发”的典型场景。

当前仓库已经具备一个可运行的最小闭环：

- 调度中心负责任务管理、Cron 扫描、路由分发、失败重试和执行日志落库
- 执行器通过注解声明任务，并在启动后自动向调度中心注册、持续发送心跳
- 调度中心与执行器之间通过 HTTP 通信
- 定时调度依赖 MySQL 持久化任务配置，依赖 Redis 做分布式锁

项目更适合作为学习型项目、内部基础版调度平台原型，或者二次开发的起点。目前提供的是后端能力和示例执行器，不包含前端管理页面、权限体系和完整的生产治理能力。

## 核心能力

- 任务管理：支持任务新增、修改、查询、启停
- 手动触发：支持按 `jobId` 立即触发任务
- 自动调度：支持 Cron 表达式校验、下次触发时间计算、后台按秒扫描触发
- 执行器注册：执行器启动后自动注册，随后每 5 秒发送一次心跳
- 在线节点管理：调度中心维护执行器在线列表，超过 30 秒无心跳会标记离线
- 路由策略：支持轮询、随机、首个、末尾、一致散列式 Hash、分片广播
- 阻塞策略：支持排队等待、丢弃新任务、覆盖运行中任务
- 失败重试：调度失败时可按任务配置进行重试
- 执行日志：记录每次触发的执行状态、耗时、错误信息
- 分布式调度保护：调度扫描阶段通过 Redis 锁避免同一任务被重复触发
- 执行器 SDK：提供 `@EnableHaloJobExecutor`、`@HaloJob`、`HaloJobContext` 等接入能力

## 架构说明

```text
                +----------------------+
                |   halo-job-admin     |
                |  调度中心 / API /    |
                |  Cron 扫描 / 路由     |
                +----------+-----------+
                           |
                           | HTTP 调度
                           v
        +------------------+------------------+
        |                                     |
+-------+--------+                    +-------+--------+
| halo-job-exec  |                    | halo-job-exec  |
| Executor A     |                    | Executor B     |
| @HaloJob tasks |                    | @HaloJob tasks |
+-------+--------+                    +-------+--------+
        ^                                     ^
        | 注册 / 心跳                          | 注册 / 心跳
        +------------------+------------------+
                           |
                           v
                +----------------------+
                |   halo-job-admin     |
                | executor_info 表      |
                +----------------------+
```

admin 依赖：

- MySQL：任务定义、执行器信息、执行日志
- Redis：定时扫描互斥锁

## 模块结构

```text
halo-job
├── halo-job-admin/      调度中心
├── halo-job-core/       公共契约 + 执行器接入 SDK
├── halo-job-executor/   示例执行器
├── sql/                 数据库初始化脚本
└── pom.xml              Maven 聚合工程
```

### `halo-job-admin`

调度中心，主要职责：

- 提供任务管理接口
- 扫描到期任务并触发执行
- 根据路由策略选择执行器
- 记录执行日志
- 维护执行器在线状态

主要入口：

- `com.zorroe.cloud.job.admin.HaloJobAdminApplication`

### `halo-job-core`

公共模块，主要职责：

- 统一响应结构 `Result`
- 任务状态、路由策略、阻塞策略等枚举
- 执行器自动注册组件
- `@HaloJob` 注解扫描与方法注册
- 执行器 HTTP 入口 `/executor/run`

### `halo-job-executor`

示例执行器，展示如何接入调度系统：

- 使用 `@EnableHaloJobExecutor` 开启接入能力
- 通过 `@HaloJob("handlerName")` 声明任务
- 启动后自动注册到调度中心

示例任务位于：

- `com.zorroe.cloud.job.executor.schedule.DemoJobTemplate`

当前内置了这些 demo handler：

- `demoNoArgTask`
- `dataSyncTask`
- `clearLogTask`
- `reportShardTask`

## 技术栈

- JDK 17
- Spring Boot 3.5.13
- Maven
- MyBatis
- MySQL
- Redis
- Druid
- Quartz CronExpression
- Lombok
- Hutool

## 已实现的任务模型

`job_info` 目前支持的关键字段如下：

| 字段 | 说明 |
| --- | --- |
| `job_name` | 任务名称 |
| `executor_handler` | 执行器 handler 名称，需要和 `@HaloJob` 的值一致 |
| `executor_param` | 执行参数 |
| `job_status` | 任务状态，`0=停止`，`1=运行` |
| `cron_expression` | Cron 表达式；为空时只支持手动触发 |
| `next_execute_time` | 下次执行时间，服务端自动计算 |
| `route_strategy` | 路由策略 |
| `block_strategy` | 阻塞策略 |
| `retry_count` | 调度失败重试次数 |
| `remark` | 备注 |

### 路由策略

| 编码 | 枚举 | 说明 |
| --- | --- | --- |
| `1` | `ROUND` | 轮询 |
| `2` | `RANDOM` | 随机 |
| `3` | `FIRST` | 选择首个在线执行器 |
| `4` | `LAST` | 选择最后一个在线执行器 |
| `5` | `HASH` | 基于任务信息做稳定 Hash |
| `6` | `SHARDING_BROADCAST` | 广播到所有在线执行器，并附带分片信息 |

### 阻塞策略

| 编码 | 枚举 | 说明 |
| --- | --- | --- |
| `1` | `QUEUE_WAIT` | 排队等待 |
| `2` | `DISCARD_NEW` | 当前任务忙时丢弃新触发 |
| `3` | `COVER_RUNNING` | 中断当前任务并用新任务覆盖 |

`COVER_RUNNING` 依赖业务代码对线程中断有响应，否则旧任务未必能及时退出。

## 快速启动

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 5.7+ 或 8.x
- Redis 6.x+

### 2. 初始化数据库

执行仓库中的脚本：

```bash
mysql -uroot -p < sql/halo-job-init.sql
```

脚本会创建：

- `job_info`
- `executor_info`
- `job_execution_log`

并写入一条示例任务 `dataSyncTask`。

### 3. 修改配置

请先替换仓库中现有 `application.yaml` 里的本地开发地址、用户名和密码，不要直接使用默认内容部署到其他环境。

#### `halo-job-admin/src/main/resources/application.yaml`

参考配置：

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

#### `halo-job-executor/src/main/resources/application.yaml`

参考配置：

```yaml
server:
  port: 9090

spring:
  application:
    name: halo-job-executor

executor:
  admin: http://127.0.0.1:8080
  name: halo-job-demo-executor
  address: http://127.0.0.1:9090
```

说明：

- `executor.admin`：调度中心地址
- `executor.name`：当前执行器名称
- `executor.address`：调度中心回调当前执行器时使用的地址

如果是跨机器部署，`executor.address` 必须填写为调度中心实际可访问到的地址，不能继续使用 `localhost`。

### 4. 构建项目

```bash
mvn clean package -DskipTests
```

### 5. 启动服务

推荐先启动调度中心，再启动执行器。

#### 方式一：IDE 启动

- 启动 `HaloJobAdminApplication`
- 启动 `HaloJobExecutorApplication`

#### 方式二：命令行启动

```bash
java -jar halo-job-admin/target/halo-job-admin-0.0.1-SNAPSHOT.jar
java -jar halo-job-executor/target/halo-job-executor-0.0.1-SNAPSHOT.jar
```

### 6. 验证运行

#### 查看在线执行器

```bash
curl http://127.0.0.1:8080/executor/api/onlineList
```

#### 查看任务列表

```bash
curl http://127.0.0.1:8080/admin/job/list
```

#### 手动触发任务

```bash
curl "http://127.0.0.1:8080/schedule/trigger?jobId=1"
```

#### 查询执行日志

```bash
curl http://127.0.0.1:8080/admin/job/log/list
```

## 主要接口

### 任务管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/admin/job/save` | 新增或更新任务 |
| `GET` | `/admin/job/get/{id}` | 查询任务详情 |
| `GET` | `/admin/job/list` | 查询任务列表 |
| `POST` | `/admin/job/changeStatus/{id}/{status}` | 修改任务状态 |

### 调度与日志

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/schedule/trigger?jobId={id}` | 手动触发任务 |
| `GET` | `/admin/job/log/list` | 查询执行日志 |

### 执行器管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/executor/api/register` | 执行器注册 |
| `POST` | `/executor/api/beat` | 执行器心跳 |
| `GET` | `/executor/api/onlineList` | 在线执行器列表 |

### 执行器内部接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/executor/run` | 调度中心下发标准触发请求 |
| `GET` | `/executor/run` | 兼容旧版的简单触发方式 |

## 创建一个任务

可以通过 `POST /admin/job/save` 创建任务，例如：

```json
{
  "jobName": "Demo Data Sync",
  "executorHandler": "dataSyncTask",
  "executorParam": "tenant=demo",
  "jobStatus": 1,
  "cronExpression": "0 */5 * * * ?",
  "routeStrategy": 1,
  "blockStrategy": 1,
  "retryCount": 1,
  "remark": "每 5 分钟执行一次"
}
```

说明：

- `id` 为空时新增，不为空时更新
- `cronExpression` 为空时不会自动调度，只能手动触发
- `nextExecuteTime` 由服务端根据 Cron 自动计算，无需手工传入

## 执行器接入方式

如果你要新增一个业务执行器，最简单的方式是直接参考 `halo-job-executor` 模块。

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
        System.out.println("param=" + param + ", shard=" + (context.getShardIndex() + 1) + "/" + context.getShardTotal());
    }
}
```

### 3. 支持的方法签名

当前 `@HaloJob` 支持以下四种签名：

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

### 4. `HaloJobContext` 可获取的信息

- `jobId`
- `jobName`
- `handler`
- `param`
- `shardIndex`
- `shardTotal`
- `executorAddress`
- `triggerType`

这对分片任务、日志补充、根据触发来源做差异处理都很有用。

## 调度流程

1. 执行器启动，自动向调度中心注册并持续发送心跳
2. 调度中心从 `job_info` 中扫描到期任务
3. 调度中心用 Redis 锁避免同一任务被重复调度
4. 调度中心根据路由策略选择一个或多个在线执行器
5. 调度中心通过 `/executor/run` 下发标准请求
6. 执行器按阻塞策略串行化同一任务的执行
7. 执行完成后，调度中心记录执行日志

## 注意事项

- `executor_handler` 必须和 `@HaloJob("...")` 的值完全一致
- 执行器是否可调度，取决于 `executor.address` 是否能被 admin 访问
- 自动调度依赖 Redis；如果 Redis 不可用，定时扫描互斥能力会失效
- `retry_count` 只对调度失败场景有意义，不是业务级幂等重试方案
- `COVER_RUNNING` 会尝试中断正在执行的线程，业务代码应正确响应中断
- 当前日志接口返回全量列表，尚未实现分页
- 当前项目没有鉴权、租户隔离和管理界面，生产使用前需要补充

## 后续可扩展方向

- 管理后台 UI
- 权限认证与审计
- 日志分页与检索
- 执行结果回调增强
- 告警通知
- 任务依赖编排
- 更完整的集群治理能力

## License

本项目当前更适合作为学习与二次开发参考，请结合你的实际场景补充部署、安全与治理能力。
