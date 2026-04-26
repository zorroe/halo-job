# Halo-Job 迭代路线图

本文档描述 `halo-job` 后续开发顺序。当前主分支基线是干净版 `v0.2`，不再为旧表结构、旧字段和旧接口保留兼容层。

## 设计原则

- 先把数据模型和协议做对，再扩功能。
- 任务定义阶段尽量拦住错误，不把明显错误留到运行时。
- 每个版本都必须是可编译、可启动、可验证的闭环。
- 不为了历史包袱牺牲模型清晰度。

## 当前基线：v0.2

`v0.2` 已完成这些基础能力：

- 执行器按 `group + app` 注册
- 注册时同步 handler 元数据，心跳只做在线续约
- admin 维护 `executor_handler` 注册表
- 任务定义升级为 `trigger_type + trigger_config`
- 任务保存时强校验 `group + app + handler`
- 调度阶段按 `group + app + handler` 过滤在线执行器
- 支持 `CRON / ONCE / FIXED_RATE / FIXED_DELAY`

`v0.2` 有意不做：

- Misfire
- 超时和取消
- 任务实例模型
- JSON 参数模型
- SPI 化策略扩展

## 版本总览

| 版本 | 目标 | 关键词 |
| --- | --- | --- |
| `v0.3` | 补齐运行时能力 | Misfire、instance、attempt、超时、取消、重跑 |
| `v0.4` | 建立扩展架构 | SPI、JSON 参数、元数据上报、多任务类型 |
| `v1.0` | 平台化治理 | 编排、告警、权限、审计、观测 |

## v0.3 运行时能力

### 目标

解决“任务触发后如何可靠运行、如何追踪一次执行、如何处理超时和失败”的问题。

### 范围

- Misfire 策略
- `job_instance`
- `job_attempt`
- 超时控制
- 取消运行中任务
- 按实例重跑
- 日志分页和条件过滤

### 建议数据模型

#### `job_instance`

| 字段 | 说明 |
| --- | --- |
| `id` | 实例 ID |
| `job_id` | 任务 ID |
| `trigger_type` | 触发来源 |
| `scheduled_time` | 计划触发时间 |
| `actual_trigger_time` | 实际触发时间 |
| `status` | `WAITING/RUNNING/SUCCESS/FAILED/CANCELED/TIMEOUT` |
| `dispatch_count` | 下发次数 |
| `final_result` | 最终结果摘要 |
| `create_time` | 创建时间 |
| `update_time` | 更新时间 |

#### `job_attempt`

| 字段 | 说明 |
| --- | --- |
| `id` | attempt ID |
| `instance_id` | 对应实例 |
| `executor_address` | 实际执行节点 |
| `shard_index` | 分片序号 |
| `shard_total` | 分片总数 |
| `attempt_no` | 第几次尝试 |
| `status` | 尝试状态 |
| `error_msg` | 错误信息 |
| `start_time` | 开始时间 |
| `end_time` | 结束时间 |
| `cost_time` | 耗时 |

### 关键改造

- 触发前先创建 `job_instance`
- 每次下发和重试都落一条 `job_attempt`
- 启动扫描时按 Misfire 策略补偿
- 超时和取消都回写实例与 attempt 状态
- 现有 `job_execution_log` 逐步退化为查询视图或历史表

### 验收标准

- 能区分一次任务实例和多次 attempt
- 超时任务会被标记为 `TIMEOUT`
- 取消任务后状态能准确回写
- 日志接口支持分页和条件查询
- admin 重启后能按 Misfire 策略恢复调度

## v0.4 扩展架构

### 目标

把现在写死在枚举和 `switch` 里的能力，演进成可扩展架构。

### 范围

- 策略 SPI
- JSON 参数模型
- handler 元数据上报增强
- 多任务类型

### 关键改造

- 把路由、阻塞、重试、Misfire、触发解析抽成接口
- `executor_param` 从纯字符串升级为 JSON
- 给 `@HaloJob` 增加更完整的元数据
- 除 `JAVA_HANDLER` 外，新增 `HTTP_CALLBACK`，后续预留 `SHELL_SCRIPT`

### 目标接口

- `RouteStrategy`
- `BlockStrategy`
- `RetryPolicy`
- `MisfirePolicy`
- `TriggerTypeHandler`
- `ExecutionResultAggregator`

### 验收标准

- 新增一个策略不需要改核心 `switch`
- 新增一种触发类型只需实现接口并注册
- 新任务可以使用 JSON 参数
- admin 可查询 handler 上报的元数据
- HTTP 类型任务能走统一调度入口

## v1.0 平台化治理

### 目标

从“可运行调度器”升级为“可治理任务平台”。

### 范围

- DAG 编排
- 告警通知
- 权限认证
- 审计和发布历史
- 指标观测
- 平台治理能力

### 关键能力

- 前置依赖和 DAG
- 失败分支和补偿任务
- 任务失败、超时、执行器离线告警
- 登录认证和分组级权限
- 配置变更审计
- 成功率、耗时、积压、在线节点等指标
- 执行器摘除、任务限流、手工 reroute

### 验收标准

- 复杂任务链路可配置执行
- 告警、权限、审计形成闭环
- 核心运行指标可查询可展示
- 执行器异常时具备基本止损能力

## 推荐开发顺序

1. 先做 `v0.3` 数据模型和运行时状态流转。
2. 再做 `v0.3` 的取消、超时和日志查询闭环。
3. `v0.4` 再开始拆 SPI 和参数模型。
4. `v1.0` 最后补平台治理能力。

## 每个版本的交付要求

- 代码
- SQL 脚本
- README 和 ROADMAP 更新
- 接口文档
- 最小可验证步骤

下一步开发直接从 `v0.3` 开始，不再回头维护旧模型。
