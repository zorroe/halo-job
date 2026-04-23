# Demo Executor 模板

`halo-job-executor` 现在既是一个可运行的 demo，也是一份可以直接复制的新 executor 模板。

如果你要新接一个执行器服务，最简单的方式就是复制这个模块，然后替换：

- 包名
- 应用名
- `executor.name`
- `executor.address`
- 示例任务类

## 1. 这个模块包含什么

- 启动类：启用 `@EnableHaloJobExecutor`
- 示例配置：展示 executor 接入调度中心所需的最小配置
- 示例任务：覆盖常见的任务方法签名和分片上下文用法

## 2. 快速复制步骤

1. 复制 `halo-job-executor` 为你的新模块
2. 修改 `pom.xml` 中的 `artifactId`
3. 修改启动类包名和应用名
4. 修改 `src/main/resources/application.yaml`
5. 删除示例任务，替换成你自己的 `@HaloJob` 任务

## 3. 最少需要改哪些配置

`application.yaml` 里至少要确认这几个值：

```yaml
server:
  port: 9090

spring:
  application:
    name: halo-job-executor

executor:
  admin: http://localhost:8080
  name: halo-job-demo-executor
  address: http://127.0.0.1:9090
```

建议：

- 本地开发时可以直接用 `127.0.0.1`
- 部署到测试或生产环境时，`executor.address` 改成 admin 可访问到的实际地址

## 4. 示例任务说明

当前示例类在 [`DemoJobTemplate.java`](/E:/code/halo-job/halo-job-executor/src/main/java/com/zorroe/cloud/job/executor/schedule/DemoJobTemplate.java)。

它演示了：

- 无参任务
- 只接收 `String param`
- 只接收 `HaloJobContext`
- 同时接收 `String param + HaloJobContext`

你可以直接删掉整个示例类，然后新增自己的任务类：

```java
@Component
public class UserSyncJob {

    @HaloJob("userSyncTask")
    public void sync(String param) {
        // your business code
    }
}
```

## 5. 适合保留的模板结构

建议你的执行器模块至少保留这些结构：

```text
src/main/java
  └─ 启动类
  └─ job/ 或 schedule/ 下的任务类

src/main/resources
  └─ application.yaml
```

## 6. 配合 admin 使用时要注意

- admin 中配置的 `executor_handler` 必须和 `@HaloJob("handlerName")` 完全一致
- 如果任务是广播分片任务，业务代码里建议读取 `HaloJobContext`
- 如果任务使用覆盖策略，业务代码应尽量响应线程中断

## 7. 推荐复制后做的第一件事

建议先把这几个值改掉，避免多个 demo executor 在同一环境下互相冲突：

- `spring.application.name`
- `executor.name`
- `server.port`
- `executor.address`
