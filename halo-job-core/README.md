# Halo-Job Core 接入说明

`halo-job-core` 是给执行器使用的共享契约 + 轻量执行器 SDK。

它负责提供：

- `@EnableHaloJobExecutor`：一键接入执行器运行时
- `@HaloJob`：声明任务 handler
- `/executor/run`：执行器接收调度请求的统一入口
- 执行器自动注册与心跳上报
- 任务上下文、阻塞策略、触发请求/响应模型

如果你要接入一个新的 executor 服务，目标应该是：

1. 引入 `halo-job-core`
2. 添加少量配置
3. 编写带 `@HaloJob` 的业务任务
4. 启动后自动注册到调度中心

## 1. 最小接入步骤

### 1.1 添加依赖

```xml
<dependency>
    <groupId>com.zorroe.cloud</groupId>
    <artifactId>halo-job-core</artifactId>
    <version>${halo-job.version}</version>
</dependency>
```

`halo-job-core` 已经依赖了 `spring-boot-starter-web`，如果你的执行器是普通 Spring Boot Web 服务，通常不需要再重复引入一套执行入口代码。

### 1.2 启用执行器能力

在启动类上添加 `@EnableHaloJobExecutor`：

```java
package com.example.demoexecutor;

import com.zorroe.cloud.job.core.anno.EnableHaloJobExecutor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableHaloJobExecutor
@SpringBootApplication
public class DemoExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoExecutorApplication.class, args);
    }
}
```

### 1.3 添加执行器配置

```yaml
server:
  port: 9091

spring:
  application:
    name: demo-executor

executor:
  admin: http://localhost:8080
  name: demo-executor
  address: http://127.0.0.1:9091
```

配置说明：

- `executor.admin`：调度中心地址，执行器会向它注册并持续发送心跳
- `executor.name`：执行器名称，建议在环境内保持可读且稳定
- `executor.address`：调度中心回调当前执行器时使用的地址

建议在线上环境始终显式配置 `executor.address`，不要依赖默认的 `http://localhost:{port}`。

### 1.4 编写任务

```java
package com.example.demoexecutor.job;

import com.zorroe.cloud.job.core.anno.HaloJob;
import com.zorroe.cloud.job.core.context.HaloJobContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoOrderJob {

    @HaloJob("orderSyncTask")
    public void sync(String param, HaloJobContext context) {
        log.info(
                "sync order data, param={}, shard={}/{}",
                param,
                context.getShardIndex() + 1,
                context.getShardTotal()
        );
    }
}
```

启动后，执行器会自动：

- 扫描所有 `@HaloJob`
- 暴露 `/executor/run`
- 向 admin 注册
- 周期性发送心跳

## 2. 支持的任务方法签名

当前执行器支持以下 4 种方法签名：

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

推荐优先使用：

- 简单任务：`String param`
- 分片或需要上下文的任务：`String param, HaloJobContext context`

## 3. HaloJobContext 能拿到什么

`HaloJobContext` 目前会携带这些运行时信息：

- `jobId`
- `jobName`
- `handler`
- `param`
- `shardIndex`
- `shardTotal`
- `executorAddress`
- `triggerType`

适合用于：

- 分片任务
- 记录更细的业务日志
- 在同一 handler 内根据触发来源做细分控制

## 4. 调度中心如何控制执行器

接入方通常不用关心底层实现，但了解这几个点会更容易排障：

- 路由策略在 `admin` 侧决定
- 阻塞策略在 `executor` 侧生效
- 分片广播由 `admin` 拆分后下发到多个执行器
- `executor` 收到结构化请求后再反射调用 `@HaloJob` 方法

这意味着：

- 如果任务发到了错误的机器，优先看 admin 的路由配置
- 如果任务被丢弃、覆盖、排队，优先看 executor 的阻塞策略

## 5. 当前内置能力

### 5.1 路由策略

`admin` 已支持这些策略：

- `ROUND`
- `FIRST`
- `LAST`
- `RANDOM`
- `HASH`
- `SHARDING_BROADCAST`

### 5.2 阻塞策略

`executor` 已支持这些策略：

- `QUEUE_WAIT`
- `DISCARD_NEW`
- `COVER_RUNNING`

如果你希望“新任务覆盖旧任务”，业务代码最好能正确响应线程中断。

## 6. 常见接入问题

### 6.1 执行器注册成功，但调度失败

优先检查：

- `executor.address` 是否能被 admin 访问
- admin 与 executor 之间是否网络互通
- handler 名称是否和任务配置里的 `executor_handler` 一致

### 6.2 任务被触发了，但只在本机日志里看到一部分分片

优先检查：

- 任务是不是使用了 `SHARDING_BROADCAST`
- 当前在线执行器数量是否和预期一致
- 业务代码是否使用了 `HaloJobContext` 中的分片参数

### 6.3 覆盖策略不生效

`COVER_RUNNING` 的本质是中断旧任务并安排新任务执行。

如果你的任务代码完全不响应中断，比如长时间阻塞调用没有检查中断状态，那旧任务可能不会及时退出。

## 7. 推荐接入方式

最推荐的接入方式是：

1. 直接参考仓库里的 [`halo-job-executor`](/E:/code/halo-job/halo-job-executor/pom.xml) 模块
2. 复制它作为新的业务执行器项目骨架
3. 替换包名、应用名、执行器名和示例任务

配套模板说明见 [`halo-job-executor/README.md`](/E:/code/halo-job/halo-job-executor/README.md)。
