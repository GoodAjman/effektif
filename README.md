# Effektif 工作流引擎源码分析

_Discontinued: The engine source code is still available, but
the original creators will not develop this codebase further_

Unfortunately, our benefit from the open source project was too low to justify the extra effort that resulted from splitting our product in an open source and product components.

Meanwhile, development continues and the Effektif product is now available as [Signavio Workflow](http://www.signavio.com/products/workflow/).

---

## 📚 源码阅读指南

本文档提供了 Effektif 工作流引擎的完整源码分析，帮助开发者深入理解现代工作流引擎的架构设计和实现原理。

### 🎯 学习目标

通过阅读本项目源码，您将学习到：
- 工作流引擎的核心架构设计
- 经典设计模式在实际项目中的应用
- 分布式系统的并发控制机制
- 模块化架构的最佳实践
- MongoDB 在工作流引擎中的应用

### 🏗️ 项目架构概览

Effektif 采用分层模块化架构，主要包含以下模块：

```
effektif/
├── effektif-workflow-api/      # API层 - 核心接口和数据模型
├── effektif-workflow-impl/     # 实现层 - 核心业务逻辑
├── effektif-mongo/            # 存储层 - MongoDB持久化实现
├── effektif-server/           # 服务层 - REST API服务器
├── effektif-adapter/          # 扩展层 - 适配器框架
├── effektif-adapter-activity/ # 扩展层 - 活动适配器
├── effektif-email/           # 扩展层 - 邮件服务
├── effektif-script/          # 扩展层 - 脚本执行
└── effektif-examples/        # 示例代码
```

### 📖 源码阅读步骤

#### 第一步：理解核心概念

1. **工作流引擎接口** (`effektif-workflow-api/src/main/java/com/effektif/workflow/api/WorkflowEngine.java`)
   - 工作流引擎的门面接口
   - 提供工作流部署、启动、执行的核心方法
   - 采用门面模式设计

2. **可执行工作流** (`effektif-workflow-api/src/main/java/com/effektif/workflow/api/workflow/ExecutableWorkflow.java`)
   - 工作流定义的API表示
   - 支持链式构建（建造者模式）
   - 支持多种序列化格式

3. **活动基类** (`effektif-workflow-api/src/main/java/com/effektif/workflow/api/workflow/Activity.java`)
   - 所有活动类型的基类
   - 定义输入输出参数
   - 支持流转定义

#### 第二步：深入核心实现

1. **工作流引擎实现** (`effektif-workflow-impl/src/main/java/com/effektif/workflow/impl/WorkflowEngineImpl.java`)
   - WorkflowEngine接口的主要实现
   - 协调各个组件完成工作流操作
   - 实现并发控制和事务管理

2. **依赖注入容器** (`effektif-workflow-impl/src/main/java/com/effektif/workflow/impl/configuration/Brewery.java`)
   - 轻量级IoC容器实现
   - 支持延迟初始化和生命周期管理
   - 组件依赖关系解析

3. **工作流解析器** (`effektif-workflow-impl/src/main/java/com/effektif/workflow/impl/WorkflowParser.java`)
   - 解析和验证工作流定义
   - 编译为可执行的内部表示
   - 错误收集和报告

#### 第三步：存储层分析

1. **存储抽象接口**
   - `WorkflowStore.java` - 工作流定义存储
   - `WorkflowInstanceStore.java` - 工作流实例存储
   - `JobStore.java` - 定时任务存储

2. **MongoDB实现** (`effektif-mongo/`)
   - `MongoWorkflowStore.java` - MongoDB工作流存储
   - `MongoWorkflowInstanceStore.java` - MongoDB实例存储
   - 支持原子操作和锁机制

3. **内存实现** (`effektif-workflow-impl/src/main/java/com/effektif/workflow/impl/memory/`)
   - 用于测试和开发环境
   - 简单的Map-based实现

#### 第四步：活动类型系统

1. **活动类型接口** (`effektif-workflow-impl/src/main/java/com/effektif/workflow/impl/activity/ActivityType.java`)
   - 定义活动的解析和执行逻辑
   - 支持自定义活动类型

2. **内置活动类型** (`effektif-workflow-api/src/main/java/com/effektif/workflow/api/activities/`)
   - `StartEvent.java` - 开始事件
   - `EndEvent.java` - 结束事件
   - `ReceiveTask.java` - 接收任务
   - `ExclusiveGateway.java` - 排他网关
   - `ParallelGateway.java` - 并行网关

#### 第五步：扩展机制

1. **适配器框架** (`effektif-adapter/`)
   - 支持外部系统集成
   - 动态活动类型注册

2. **脚本执行** (`effektif-script/`)
   - JavaScript脚本支持
   - 动态业务逻辑执行

### 🔍 关键设计模式分析

#### 1. 门面模式 (Facade Pattern)
- **应用位置**: `WorkflowEngine` 接口
- **作用**: 为复杂的工作流子系统提供统一的访问接口
- **优势**: 简化客户端调用，隐藏内部复杂性

#### 2. 建造者模式 (Builder Pattern)
- **应用位置**: `ExecutableWorkflow` 类
- **作用**: 支持链式调用构建复杂的工作流定义
- **优势**: 提高代码可读性，支持可选参数

#### 3. 策略模式 (Strategy Pattern)
- **应用位置**: 存储层实现 (`MemoryStore` vs `MongoStore`)
- **作用**: 支持不同的存储策略
- **优势**: 易于扩展新的存储实现

#### 4. 依赖注入模式 (Dependency Injection)
- **应用位置**: `Brewery` 容器
- **作用**: 管理组件依赖关系
- **优势**: 降低耦合度，提高可测试性

#### 5. 观察者模式 (Observer Pattern)
- **应用位置**: `WorkflowExecutionListener`
- **作用**: 监控工作流执行过程
- **优势**: 支持审计和监控功能

### 🚀 快速开始

#### 环境要求
- Java 8+
- Maven 3.6+
- MongoDB 3.6+ (可选，用于持久化)

#### 运行示例

1. **克隆项目**
```bash
git clone https://github.com/effektif/effektif.git
cd effektif
```

2. **编译项目**
```bash
mvn clean compile
```

3. **运行内存版本示例**
```java
// 创建内存配置
Configuration configuration = new MemoryConfiguration();
configuration.start();
WorkflowEngine workflowEngine = configuration.getWorkflowEngine();

// 创建工作流
ExecutableWorkflow workflow = new ExecutableWorkflow()
  .sourceWorkflowId("hello-world")
  .activity("start", new StartEvent()
    .transitionTo("task"))
  .activity("task", new ReceiveTask()
    .name("Hello Task")
    .transitionTo("end"))
  .activity("end", new EndEvent());

// 部署工作流
WorkflowId workflowId = workflowEngine
  .deployWorkflow(workflow)
  .checkNoErrorsAndNoWarnings()
  .getWorkflowId();

// 启动工作流实例
WorkflowInstance instance = workflowEngine
  .start(new TriggerInstance().workflowId(workflowId));
```

4. **运行MongoDB版本**
```java
// 创建MongoDB配置
MongoConfiguration configuration = new MongoConfiguration()
  .server("localhost", 27017)
  .databaseName("effektif");
configuration.start();
WorkflowEngine workflowEngine = configuration.getWorkflowEngine();
```

### 📊 架构图表

#### 模块依赖关系图
```
┌─────────────────┐    ┌─────────────────┐
│  effektif-server │    │ effektif-examples│
└─────────┬───────┘    └─────────┬───────┘
          │                      │
          ▼                      ▼
┌─────────────────────────────────────────┐
│        effektif-workflow-api            │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│       effektif-workflow-impl            │
└─────────┬───────────────────────────────┘
          │
          ▼
┌─────────────────┐    ┌─────────────────┐
│  effektif-mongo │    │ effektif-adapter│
└─────────────────┘    └─────────────────┘
```

#### 工作流执行流程图
```
客户端 → WorkflowEngine → WorkflowParser → WorkflowStore
   ↓                                           ↓
启动实例 ← WorkflowInstance ← ActivityInstance ← WorkflowCache
   ↓                                           ↓
发送消息 → ActivityExecution → StateTransition → 持久化更新
```

### 🔧 核心组件详解

#### WorkflowEngine (工作流引擎)
- **职责**: 工作流生命周期管理
- **核心方法**:
  - `deployWorkflow()`: 部署工作流定义
  - `start()`: 启动工作流实例
  - `send()`: 发送消息推进执行
  - `cancel()`: 取消工作流实例

#### Brewery (依赖注入容器)
- **职责**: 组件依赖管理和生命周期控制
- **核心概念**:
  - `ingredient`: 未初始化的组件
  - `beer`: 已初始化的组件
  - `supplier`: 组件工厂

#### ActivityType (活动类型系统)
- **职责**: 定义活动的行为和执行逻辑
- **扩展点**: 支持自定义活动类型
- **内置类型**: StartEvent, EndEvent, ReceiveTask, Gateway等

### 🎓 学习建议

#### 初学者路径
1. 从 `effektif-examples` 开始，理解基本用法
2. 阅读 `WorkflowEngine` 接口，了解核心功能
3. 查看 `ExecutableWorkflow` 的构建方式
4. 理解基本的活动类型

#### 进阶路径
1. 深入 `WorkflowEngineImpl` 实现细节
2. 分析 `Brewery` 依赖注入机制
3. 研究存储层的抽象和实现
4. 探索扩展机制和自定义开发

#### 高级路径
1. 分析并发控制和锁机制
2. 研究性能优化策略
3. 探索分布式部署方案
4. 贡献代码或设计改进方案

### 📝 代码注释说明

项目中的核心类已经添加了详细的中文注释，包括：
- 类的职责和设计模式说明
- 方法的功能和执行流程
- 重要字段的作用和约束
- 使用示例和最佳实践

### 🤝 贡献指南

虽然原项目已停止维护，但您可以：
1. Fork 项目进行学习和实验
2. 提交 Issue 讨论技术问题
3. 分享学习心得和改进建议
4. 基于此架构开发新的工作流引擎

### 📖 详细文档

- **[源码深度分析](SOURCE_CODE_ANALYSIS.md)** - 详细的技术架构和设计模式分析
- **[UML图表文档](WORKFLOW_DIAGRAMS.md)** - 系统架构和执行流程的可视化图表
- **[执行机制分析](WORKFLOW_EXECUTION_ANALYSIS.md)** - 工作流执行的内部机制详解
- **[快速入门指南](QUICK_START_GUIDE.md)** - 从零开始的实践教程

### 📚 相关资源

- [BPMN 2.0 规范](https://www.omg.org/spec/BPMN/2.0/)
- [工作流模式](http://www.workflowpatterns.com/)
- [Signavio Workflow](http://www.signavio.com/products/workflow/) (商业版本)

### ❓ 常见问题

#### Q: 如何理解活动定义ID与活动实例ID的关系？
A: 活动定义ID（如"greet"）是工作流设计时定义的静态标识符，而活动实例ID（如"2"）是运行时动态生成的唯一标识符。一个活动定义可以对应多个活动实例（如在循环或并行场景中）。详见[执行机制分析](WORKFLOW_EXECUTION_ANALYSIS.md)。

#### Q: 为什么 ReceiveTask 需要外部消息才能完成？
A: ReceiveTask 代表需要外部输入的任务，它在执行时会进入等待状态，不会自动完成。只有通过 `workflowEngine.send()` 方法发送消息，才会触发任务完成和流程继续。

#### Q: 如何调试工作流执行过程？
A: 可以启用调试日志、使用工作流执行监听器、检查活动实例状态等方式。详见[快速入门指南](QUICK_START_GUIDE.md)的调试部分。

#### Q: 内存配置和MongoDB配置有什么区别？
A: 内存配置适用于开发和测试，数据不持久化；MongoDB配置适用于生产环境，支持数据持久化、集群部署和高可用性。

---

**注意**: 本项目仅用于学习和研究目的，生产环境请考虑使用活跃维护的工作流引擎项目。
