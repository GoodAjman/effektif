# Effektif 工作流引擎快速入门指南

本指南将帮助您快速上手 Effektif 工作流引擎，从基本概念到实际应用。

## 📋 目录
- [环境准备](#环境准备)
- [基本概念](#基本概念)
- [第一个工作流](#第一个工作流)
- [常用活动类型](#常用活动类型)
- [变量和数据](#变量和数据)
- [条件和网关](#条件和网关)
- [错误处理](#错误处理)
- [最佳实践](#最佳实践)

## 🛠️ 环境准备

### 系统要求
- Java 8 或更高版本
- Maven 3.6+
- MongoDB 3.6+ (可选，用于持久化存储)

### 项目依赖
```xml
<dependencies>
    <!-- 核心API -->
    <dependency>
        <groupId>com.effektif</groupId>
        <artifactId>effektif-workflow-api</artifactId>
        <version>3.0.0-beta15-SNAPSHOT</version>
    </dependency>
    
    <!-- 核心实现 -->
    <dependency>
        <groupId>com.effektif</groupId>
        <artifactId>effektif-workflow-impl</artifactId>
        <version>3.0.0-beta15-SNAPSHOT</version>
    </dependency>
    
    <!-- MongoDB存储 (可选) -->
    <dependency>
        <groupId>com.effektif</groupId>
        <artifactId>effektif-mongo</artifactId>
        <version>3.0.0-beta15-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## 📚 基本概念

### 核心组件
- **WorkflowEngine**: 工作流引擎主接口
- **ExecutableWorkflow**: 可执行的工作流定义
- **Activity**: 工作流中的活动节点
- **Transition**: 活动间的流转连接
- **WorkflowInstance**: 工作流的运行实例

### 生命周期
1. **设计阶段**: 定义工作流结构
2. **部署阶段**: 将工作流部署到引擎
3. **执行阶段**: 启动和推进工作流实例
4. **监控阶段**: 跟踪工作流执行状态

## 🚀 第一个工作流

### 1. 创建工作流引擎
```java
import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.impl.memory.MemoryConfiguration;

// 创建内存配置 (适用于开发和测试)
Configuration configuration = new MemoryConfiguration();
configuration.start();
WorkflowEngine workflowEngine = configuration.getWorkflowEngine();
```

### 2. 定义简单工作流
```java
import com.effektif.workflow.api.activities.*;
import com.effektif.workflow.api.workflow.ExecutableWorkflow;

ExecutableWorkflow workflow = new ExecutableWorkflow()
    .sourceWorkflowId("hello-world")
    .name("Hello World 工作流")
    .description("我的第一个工作流")
    
    // 定义活动
    .activity("start", new StartEvent()
        .name("开始")
        .transitionTo("greet"))
        
    .activity("greet", new ReceiveTask()
        .name("问候任务")
        .transitionTo("end"))
        
    .activity("end", new EndEvent()
        .name("结束"));
```

### 3. 部署工作流
```java
import com.effektif.workflow.api.model.Deployment;
import com.effektif.workflow.api.model.WorkflowId;

Deployment deployment = workflowEngine.deployWorkflow(workflow);

if (deployment.hasErrors()) {
    System.out.println("部署失败: " + deployment.getIssues());
} else {
    WorkflowId workflowId = deployment.getWorkflowId();
    System.out.println("部署成功，工作流ID: " + workflowId);
}
```

### 4. 启动工作流实例
```java
import com.effektif.workflow.api.model.TriggerInstance;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;

WorkflowInstance instance = workflowEngine.start(
    new TriggerInstance()
        .workflowId(deployment.getWorkflowId())
        .data("message", "Hello, Effektif!")
);

System.out.println("工作流实例已启动: " + instance.getId());
```

### 5. 推进工作流执行
```java
import com.effektif.workflow.api.model.Message;

// 查找等待中的活动实例
String activityInstanceId = instance.findOpenActivityInstance("greet").getId();

// 发送消息完成任务
WorkflowInstance updatedInstance = workflowEngine.send(
    new Message()
        .workflowInstanceId(instance.getId())
        .activityInstanceId(activityInstanceId)
        .data("response", "任务已完成")
);

System.out.println("工作流状态: " + updatedInstance.isEnded() ? "已完成" : "进行中");
```

## 🎯 常用活动类型

### StartEvent - 开始事件
```java
new StartEvent()
    .name("流程开始")
    .transitionTo("nextActivity")
```

### EndEvent - 结束事件
```java
new EndEvent()
    .name("流程结束")
    .endWorkflow(true)  // 结束整个工作流
```

### ReceiveTask - 接收任务
```java
new ReceiveTask()
    .name("等待用户输入")
    .transitionTo("nextActivity")
```

### JavaServiceTask - Java服务任务
```java
new JavaServiceTask()
    .name("调用Java方法")
    .className("com.example.MyService")
    .methodName("processData")
    .inputExpression("input", "variableName")
    .transitionTo("nextActivity")
```

### HttpServiceTask - HTTP服务任务
```java
new HttpServiceTask()
    .name("调用REST API")
    .url("https://api.example.com/data")
    .method("POST")
    .header("Content-Type", "application/json")
    .bodyExpression("requestData")
    .transitionTo("nextActivity")
```

## 📊 变量和数据

### 定义工作流变量
```java
import com.effektif.workflow.api.types.*;
import com.effektif.workflow.api.workflow.Variable;

ExecutableWorkflow workflow = new ExecutableWorkflow()
    .sourceWorkflowId("data-workflow")
    
    // 定义变量
    .variable(new Variable()
        .id("userName")
        .type(new TextType())
        .name("用户姓名"))
        
    .variable(new Variable()
        .id("age")
        .type(new NumberType())
        .name("年龄"))
        
    .variable(new Variable()
        .id("isActive")
        .type(new BooleanType())
        .name("是否激活"));
```

### 在活动中使用变量
```java
new ReceiveTask()
    .name("收集用户信息")
    .inputParameter("currentUser", "userName")  // 输入参数
    .outputParameter("result", "processResult") // 输出参数
    .transitionTo("nextActivity")
```

### 设置和获取变量值
```java
import com.effektif.workflow.api.model.VariableValues;

// 设置变量值
VariableValues variables = new VariableValues()
    .value("userName", "张三")
    .value("age", 30)
    .value("isActive", true);

workflowEngine.setVariableValues(instance.getId(), variables);

// 获取变量值
VariableValues currentValues = workflowEngine.getVariableValues(instance.getId());
String userName = (String) currentValues.getValue("userName");
```

## 🔀 条件和网关

### ExclusiveGateway - 排他网关
```java
import com.effektif.workflow.api.condition.*;

ExecutableWorkflow workflow = new ExecutableWorkflow()
    .activity("decision", new ExclusiveGateway()
        .name("年龄判断")
        .defaultTransitionId("adult"))
        
    // 条件流转
    .transition(new Transition()
        .fromId("decision")
        .toId("minor")
        .condition(new LessThan()
            .leftExpression("age")
            .rightValue(18)))
            
    .transition(new Transition()
        .id("adult")
        .fromId("decision")
        .toId("adultTask"))
        
    .activity("minor", new ReceiveTask().name("未成年处理"))
    .activity("adultTask", new ReceiveTask().name("成年人处理"));
```

### ParallelGateway - 并行网关
```java
ExecutableWorkflow workflow = new ExecutableWorkflow()
    .activity("fork", new ParallelGateway()
        .name("并行分叉"))
        
    .activity("task1", new ReceiveTask()
        .name("任务1")
        .transitionTo("join"))
        
    .activity("task2", new ReceiveTask()
        .name("任务2")
        .transitionTo("join"))
        
    .activity("join", new ParallelGateway()
        .name("并行合并")
        .transitionTo("end"))
        
    // 定义流转
    .transition(new Transition().fromId("fork").toId("task1"))
    .transition(new Transition().fromId("fork").toId("task2"));
```

## ⚠️ 错误处理

### 边界事件处理
```java
import com.effektif.workflow.api.workflow.BoundaryEvent;

new ReceiveTask()
    .name("可能失败的任务")
    .boundaryEvent(new BoundaryEvent()
        .errorType("TimeoutError")
        .transitionTo("errorHandler"))
    .transitionTo("success");
```

### 重试机制
```java
new JavaServiceTask()
    .name("网络调用")
    .className("com.example.NetworkService")
    .methodName("callRemoteAPI")
    .retryPolicy(new RetryPolicy()
        .maxRetries(3)
        .retryDelay(Duration.ofSeconds(5)))
    .transitionTo("nextActivity");
```

## 💡 最佳实践

### 1. 工作流设计原则
- **保持简单**: 避免过于复杂的流程设计
- **职责单一**: 每个活动只负责一个明确的功能
- **可读性**: 使用有意义的活动和变量名称
- **可测试**: 设计易于测试的工作流结构

### 2. 性能优化
```java
// 使用异步执行
Configuration config = new MemoryConfiguration()
    .synchronous(false);  // 启用异步执行

// 合理设置缓存
config.ingredient(new SimpleWorkflowCache()
    .maxSize(1000)
    .expireAfterAccess(Duration.ofHours(1)));
```

### 3. 错误处理策略
```java
// 全局错误处理
ExecutableWorkflow workflow = new ExecutableWorkflow()
    .errorHandler(new ErrorHandler()
        .onError("ValidationError", "validationErrorHandler")
        .onError("NetworkError", "networkErrorHandler")
        .defaultHandler("generalErrorHandler"));
```

### 4. 监控和日志
```java
// 添加执行监听器
config.ingredient(new WorkflowExecutionListener() {
    @Override
    public void activityStarted(ActivityInstanceImpl activityInstance) {
        log.info("活动开始: {}", activityInstance.getActivity().getName());
    }
    
    @Override
    public void activityCompleted(ActivityInstanceImpl activityInstance) {
        log.info("活动完成: {}", activityInstance.getActivity().getName());
    }
});
```

### 5. 生产环境配置
```java
// MongoDB配置用于生产环境
MongoConfiguration config = new MongoConfiguration()
    .server("mongodb-server", 27017)
    .databaseName("effektif-prod")
    .authentication("username", "password", "authDB")
    .connectionPoolSize(50)
    .socketTimeout(30000);
```

## 🔗 相关资源

- [完整源码分析](SOURCE_CODE_ANALYSIS.md)
- [UML图表文档](WORKFLOW_DIAGRAMS.md)
- [API参考文档](effektif-workflow-api/)
- [示例代码](effektif-examples/)

---

**提示**: 这只是一个快速入门指南。要深入理解 Effektif 的架构和高级特性，建议阅读完整的源码分析文档。
