# Halo-Job Core 接入说明

`halo-job-core` 是执行器侧共享协议和轻量 SDK，负责：

- `@EnableHaloJobExecutor`
- `@HaloJob`
- 自动扫描并注册 handler
- 执行器注册和心跳上报
- `/executor/run` 执行入口
- 任务上下文和公共模型

如果你要接一个新的业务执行器，通常只需要：

1. 引入 `halo-job-core`
2. 增加执行器配置
3. 编写带 `@HaloJob` 的任务方法
4. 启动后自动注册到 admin

## 最小接入

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.zorroe.cloud</groupId>
    <artifactId>halo-job-core</artifactId>
    <version>${halo-job.version}</version>
</dependency>
```

### 2. 启用执行器

```java
@EnableHaloJobExecutor
@SpringBootApplication
public class DemoExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoExecutorApplication.class, args);
    }
}
```

### 3. 配置执行器

```yaml
server:
  port: 9091

spring:
  application:
    name: demo-executor

executor:
  admin: http://127.0.0.1:8080
  name: demo-executor
  group: default
  app: demo-executor
  version: 0.0.1-SNAPSHOT
  address: http://127.0.0.1:9091
```

说明：

- `executor.group` 和 `executor.app` 都是必填。
- `executor.address` 必须能被 admin 访问。

### 4. 编写任务

```java
@Component
public class DemoOrderJob {

    @HaloJob("orderSyncTask")
    public void sync(String param, HaloJobContext context) {
        System.out.println(
                "sync order data, param=" + param
                        + ", shard=" + (context.getShardIndex() + 1)
                        + "/" + context.getShardTotal()
        );
    }
}
```

启动后，执行器会自动：

- 扫描 `@HaloJob`
- 暴露 `POST /executor/run`
- 向 admin 发送注册请求
- 周期性发送轻量心跳

## 注册和心跳协议

### 注册

`POST /executor/api/register`

```json
{
  "name": "demo-executor",
  "address": "http://127.0.0.1:9091",
  "group": "default",
  "app": "demo-executor",
  "version": "0.0.1-SNAPSHOT",
  "metadata": "{\"zone\":\"local\"}",
  "handlers": [
    {
      "handlerName": "orderSyncTask",
      "description": "sync order task",
      "methodSignature": "void sync(java.lang.String,com.zorroe.cloud.job.core.context.HaloJobContext)"
    }
  ]
}
```

### 心跳

`POST /executor/api/beat`

```json
{
  "name": "demo-executor",
  "address": "http://127.0.0.1:9091",
  "group": "default",
  "app": "demo-executor",
  "version": "0.0.1-SNAPSHOT"
}
```

## 支持的方法签名

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

## HaloJobContext

运行时可获取：

- `jobId`
- `jobName`
- `handler`
- `param`
- `shardIndex`
- `shardTotal`
- `executorAddress`
- `triggerType`

## 运行机制

- 路由策略在 admin 侧决定
- 阻塞策略在 executor 侧生效
- 广播分片由 admin 拆分后下发
- executor 收到结构化请求后反射调用 `@HaloJob` 方法

## 排查要点

- 任务下发失败时，先检查 `executor.address` 是否可被 admin 访问。
- 任务找不到 handler 时，先检查 `@HaloJob("...")` 和任务配置是否一致。
- `COVER_RUNNING` 依赖业务代码正确响应线程中断。

示例模板见 [halo-job-executor/README.md](../halo-job-executor/README.md)。
