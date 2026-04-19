# Halo-Job 分布式任务调度平台

## 项目简介

Halo-Job 是一个基于 Spring Boot 3.x 开发的轻量级分布式任务调度系统，采用调度中心（Admin）和执行器（Executor）分离的架构设计，支持任务的统一管理、调度和执行。

## 技术栈

- **JDK**: 17+
- **Spring Boot**: 3.5.13
- **MyBatis**: 3.0.3
- **数据库**: MySQL
- **连接池**: Druid 1.2.20
- **构建工具**: Maven

## 项目结构

```
halo-job/
├── halo-job-admin/          # 调度中心 - 负责任务管理和调度
│   ├── controller/          # 控制器层
│   ├── service/             # 服务层
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   └── config/              # 配置类
├── halo-job-executor/       # 执行器 - 负责任务执行
│   └── controller/          # 控制器层
├── halo-job-core/           # 核心模块 - 公共组件和通用类
│   └── common/              # 通用类（Result、枚举等）
└── pom.xml                  # 父POM
```

## 模块说明

### 1. halo-job-core（核心模块）
提供公共组件和通用类：
- `Result`: 统一响应结果封装
- `ResultCode`: 响应状态码定义
- `JobStatusEnum`: 任务状态枚举（停止/运行）

### 2. halo-job-admin（调度中心）
负责任务的管理和调度：
- 任务信息的 CRUD 操作
- 手动触发任务执行
- 通过 RestTemplate 调用执行器
- 集成 MyBatis + Druid 进行数据持久化

**主要接口：**
- `GET /schedule/trigger?jobId={id}`: 手动触发指定任务

### 3. halo-job-executor（执行器）
负责接收调度中心的命令并执行具体任务：
- 接收任务执行请求
- 根据 handler 路由到具体的任务处理方法
- 返回执行结果

**主要接口：**
- `GET /executor/run?handler={name}&param={param}`: 执行指定任务

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- MySQL 5.7+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE `halo-job` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

2. 创建任务表：
```sql
CREATE TABLE `job_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称',
  `executor_handler` varchar(100) NOT NULL COMMENT '执行器处理器',
  `executor_param` varchar(500) DEFAULT NULL COMMENT '执行参数',
  `job_status` tinyint NOT NULL DEFAULT '1' COMMENT '任务状态：0-停止，1-运行',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务信息表';
```

3. 修改配置文件中的数据库连接信息（`halo-job-admin/src/main/resources/application.yaml`）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-host:port/halo-job?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: your-username
    password: your-password
```

### 启动步骤

#### 方式一：IDE 启动

1. 编译项目：
```bash
mvn clean install
```

2. 启动执行器（halo-job-executor）：
   - 运行 `HaloJobExecutorApplication.java`
   - 默认端口：9090

3. 启动调度中心（halo-job-admin）：
   - 运行 `HaloJobAdminApplication.java`
   - 默认端口：8080

#### 方式二：命令行启动

```bash
# 编译打包
mvn clean package -DskipTests

# 启动执行器
java -jar halo-job-executor/target/halo-job-executor-0.0.1-SNAPSHOT.jar

# 启动调度中心
java -jar halo-job-admin/target/halo-job-admin-0.0.1-SNAPSHOT.jar
```

### 使用示例

1. **插入测试任务数据**：
```sql
INSERT INTO job_info (job_name, executor_handler, executor_param, job_status) 
VALUES ('数据同步任务', 'dataSyncTask', 'test-param', 1);
```

2. **触发任务执行**：
```bash
curl http://localhost:8080/schedule/trigger?jobId=1
```

3. **查看日志**：
- 调度中心日志显示任务触发信息
- 执行器日志显示任务执行过程和结果

## 配置说明

### 调度中心配置（halo-job-admin）

```yaml
server:
  port: 8080                    # 调度中心端口

executor:
  address: http://localhost:9090  # 执行器地址

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://host:port/halo-job
    username: xxx
    password: xxx

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.zorroe.cloud.job.admin.entity
```

### 执行器配置（halo-job-executor）

```yaml
server:
  port: 9090                    # 执行器端口

spring:
  application:
    name: halo-job-executor
```

## 扩展开发

### 添加新任务

1. 在 `halo-job-executor` 的 `ExecutorController` 中添加新的任务处理方法：

```java
@GetMapping("/run")
public Result<String> runTask(
        @RequestParam String handler,
        @RequestParam(required = false) String param
) {
    if (handler.equals("dataSyncTask")) {
        return Result.success(dataSyncTask(param));
    } else if (handler.equals("yourNewTask")) {
        return Result.success(yourNewTask(param));
    }
    return Result.fail("未知任务处理器：" + handler);
}

private String yourNewTask(String param) {
    // 实现你的业务逻辑
    log.info("执行新任务，参数：{}", param);
    return "任务执行成功";
}
```

2. 在数据库中注册新任务：
```sql
INSERT INTO job_info (job_name, executor_handler, executor_param, job_status) 
VALUES ('新任务', 'yourNewTask', 'param-value', 1);
```

3. 通过调度中心触发任务即可。

### 自定义响应码

在 `halo-job-core` 模块的 `ResultCode` 枚举中添加新的状态码：

```java
public enum ResultCode {
    SUCCESS(200, "成功"),
    FAIL(500, "失败"),
    JOB_NOT_EXIST(404, "任务不存在"),
    JOB_STOPPED(403, "任务已停止");
    
    // ... 添加新的状态码
}
```

## 架构特点

- **模块化设计**: 核心模块、调度中心、执行器分离，便于维护和扩展
- **RESTful API**: 基于 HTTP 协议的简洁通信方式
- **统一响应**: 标准化的 Result 响应格式
- **易于扩展**: 只需在执行器中添加新的处理方法即可支持新任务
- **状态管理**: 支持任务的启停控制

## 注意事项

1. 确保执行器先于调度中心启动
2. 调度中心需要正确配置执行器的地址
3. 任务状态为"停止"时无法触发执行
4. 生产环境建议配置集群和高可用方案
5. 注意数据库连接池的配置优化

## 未来规划

- [ ] 支持 Cron 表达式定时调度
- [ ] 任务执行日志记录
- [ ] 失败重试机制
- [ ] 任务依赖关系管理
- [ ] Web 管理界面
- [ ] 执行器集群支持
- [ ] 任务分片执行
- [ ] 告警通知功能

## 许可证

本项目仅供学习和参考使用。

## 联系方式

如有问题或建议，欢迎提交 Issue。
