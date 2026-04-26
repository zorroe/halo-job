# Demo Executor 模板

`halo-job-executor` 既是可运行 demo，也是新执行器的复制模板。

如果你要新增一个执行器服务，最简单的方式就是复制这个模块，然后替换：

- 包名
- 应用名
- `executor.name`
- `executor.app`
- `executor.address`
- 示例任务类

## 模块包含内容

- 启动类：启用 `@EnableHaloJobExecutor`
- 示例配置：展示执行器接入 admin 所需的最小配置
- 示例任务：覆盖 4 种支持的方法签名

## 快速复制步骤

1. 复制 `halo-job-executor` 为你的新模块。
2. 修改 `pom.xml` 的 `artifactId`。
3. 修改启动类包名和应用名。
4. 修改 `src/main/resources/application.yaml`。
5. 删除示例任务，换成你自己的 `@HaloJob` 任务。

## 最少需要确认的配置

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

注意：

- `executor.group` 和 `executor.app` 都要和 admin 里的任务定义一致。
- `executor.address` 必须是 admin 能访问到的地址。
- 不要在同一环境里让多个 demo 执行器复用同一个地址和名称。

## 示例任务

当前示例类在 [DemoJobTemplate.java](src/main/java/com/zorroe/cloud/job/executor/schedule/DemoJobTemplate.java)。

它演示了：

- 无参任务
- `String param`
- `HaloJobContext`
- `String param + HaloJobContext`

你可以直接删除示例类，换成自己的任务：

```java
@Component
public class UserSyncJob {

    @HaloJob("userSyncTask")
    public void sync(String param) {
        // your business code
    }
}
```

## 与 admin 配合时要注意

- 任务保存时，admin 会校验 `executor_group + executor_app + executor_handler` 是否存在。
- handler 名必须和 `@HaloJob("handlerName")` 完全一致。
- 广播分片任务建议读取 `HaloJobContext` 中的分片参数。
- `COVER_RUNNING` 依赖业务代码响应线程中断。

复制后第一时间建议修改：

- `spring.application.name`
- `executor.name`
- `executor.app`
- `server.port`
- `executor.address`
