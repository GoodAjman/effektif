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

#### 5.1 理解活动ID与活动实例的关系

当工作流启动时，会发生以下过程：

```java
// 工作流定义中的活动ID
.activity("greet", new ReceiveTask()  // "greet" 是活动定义的ID
    .name("问候任务")
    .transitionTo("end"))
```

当工作流实例启动后：
1. **活动定义** (`Activity`) 是静态的模板，定义了活动的行为
2. **活动实例** (`ActivityInstance`) 是运行时的实例，基于活动定义创建
3. 每个活动实例都有自己的唯一ID，但可以通过活动定义ID来查找

```java
// 启动工作流后，系统内部发生的过程：
// 1. 根据工作流定义创建工作流实例
// 2. 执行 StartEvent，完成后自动流转到 "greet" 活动
// 3. 创建 "greet" 活动的实例 (ActivityInstance)
// 4. ReceiveTask 进入等待状态，等待外部消息
```

#### 5.2 查找和操作活动实例

```java
import com.effektif.workflow.api.model.Message;
import com.effektif.workflow.api.workflowinstance.ActivityInstance;

// 方法1: 通过活动定义ID查找活动实例
ActivityInstance greetActivityInstance = instance.findOpenActivityInstance("greet");
if (greetActivityInstance != null) {
    String activityInstanceId = greetActivityInstance.getId();
    System.out.println("找到等待中的活动实例: " + activityInstanceId);
} else {
    System.out.println("活动 'greet' 不在等待状态或已完成");
}

// 方法2: 查看所有开放的活动实例
List<ActivityInstance> openActivities = instance.getOpenActivityInstances();
for (ActivityInstance activity : openActivities) {
    System.out.println("开放活动: " + activity.getActivityId() +
                      " (实例ID: " + activity.getId() + ")");
}

// 发送消息完成任务
if (greetActivityInstance != null) {
    WorkflowInstance updatedInstance = workflowEngine.send(
        new Message()
            .workflowInstanceId(instance.getId())
            .activityInstanceId(greetActivityInstance.getId())
            .data("response", "任务已完成")
            .data("timestamp", new Date())
    );

    System.out.println("工作流状态: " + (updatedInstance.isEnded() ? "已完成" : "进行中"));
}
```

#### 5.3 完整的执行示例

```java
public class WorkflowExecutionExample {
    public static void main(String[] args) {
        // 1. 创建引擎
        Configuration configuration = new MemoryConfiguration();
        configuration.start();
        WorkflowEngine workflowEngine = configuration.getWorkflowEngine();

        // 2. 定义工作流
        ExecutableWorkflow workflow = new ExecutableWorkflow()
            .sourceWorkflowId("demo-workflow")
            .name("演示工作流")
            .activity("start", new StartEvent()
                .name("开始")
                .transitionTo("task1"))
            .activity("task1", new ReceiveTask()
                .name("第一个任务")
                .transitionTo("task2"))
            .activity("task2", new ReceiveTask()
                .name("第二个任务")
                .transitionTo("end"))
            .activity("end", new EndEvent()
                .name("结束"));

        // 3. 部署工作流
        Deployment deployment = workflowEngine.deployWorkflow(workflow);
        System.out.println("工作流部署: " + (deployment.hasErrors() ? "失败" : "成功"));

        // 4. 启动工作流实例
        WorkflowInstance instance = workflowEngine.start(
            new TriggerInstance()
                .workflowId(deployment.getWorkflowId())
                .data("initiator", "张三")
        );

        System.out.println("工作流实例已启动: " + instance.getId());
        printWorkflowStatus(instance);

        // 5. 完成第一个任务
        ActivityInstance task1Instance = instance.findOpenActivityInstance("task1");
        if (task1Instance != null) {
            System.out.println("\n完成第一个任务...");
            instance = workflowEngine.send(
                new Message()
                    .workflowInstanceId(instance.getId())
                    .activityInstanceId(task1Instance.getId())
                    .data("task1Result", "第一个任务完成")
            );
            printWorkflowStatus(instance);
        }

        // 6. 完成第二个任务
        ActivityInstance task2Instance = instance.findOpenActivityInstance("task2");
        if (task2Instance != null) {
            System.out.println("\n完成第二个任务...");
            instance = workflowEngine.send(
                new Message()
                    .workflowInstanceId(instance.getId())
                    .activityInstanceId(task2Instance.getId())
                    .data("task2Result", "第二个任务完成")
            );
            printWorkflowStatus(instance);
        }

        System.out.println("\n工作流执行完成!");
    }

    private static void printWorkflowStatus(WorkflowInstance instance) {
        System.out.println("工作流状态: " + (instance.isEnded() ? "已结束" : "进行中"));

        List<ActivityInstance> openActivities = instance.getOpenActivityInstances();
        if (!openActivities.isEmpty()) {
            System.out.println("等待中的活动:");
            for (ActivityInstance activity : openActivities) {
                System.out.println("  - " + activity.getActivityId() +
                                 " (" + activity.getId() + ")");
            }
        }

        // 显示已完成的活动
        List<ActivityInstance> endedActivities = instance.getEndedActivityInstances();
        if (!endedActivities.isEmpty()) {
            System.out.println("已完成的活动:");
            for (ActivityInstance activity : endedActivities) {
                System.out.println("  - " + activity.getActivityId());
            }
        }
    }
}
```

## 🔍 调试和监控

### 调试工作流执行

```java
public class WorkflowDebugExample {
    public static void main(String[] args) {
        // 启用调试日志
        System.setProperty("org.slf4j.simpleLogger.log.com.effektif", "debug");

        Configuration configuration = new MemoryConfiguration();
        configuration.start();
        WorkflowEngine workflowEngine = configuration.getWorkflowEngine();

        // 定义工作流
        ExecutableWorkflow workflow = new ExecutableWorkflow()
            .sourceWorkflowId("debug-workflow")
            .name("调试工作流")
            .activity("start", new StartEvent()
                .name("开始")
                .transitionTo("task"))
            .activity("task", new ReceiveTask()
                .name("等待任务")
                .transitionTo("end"))
            .activity("end", new EndEvent()
                .name("结束"));

        // 部署并启动
        Deployment deployment = workflowEngine.deployWorkflow(workflow);
        WorkflowInstance instance = workflowEngine.start(
            new TriggerInstance().workflowId(deployment.getWorkflowId())
        );

        // 详细状态检查
        System.out.println("=== 工作流启动后状态 ===");
        printDetailedStatus(instance);

        // 查找等待中的活动
        ActivityInstance waitingTask = findWaitingActivity(instance, "task");
        if (waitingTask != null) {
            System.out.println("\n=== 发送消息前 ===");
            System.out.println("等待活动: " + waitingTask.getActivityId());
            System.out.println("活动实例ID: " + waitingTask.getId());

            // 发送消息
            WorkflowInstance updatedInstance = workflowEngine.send(
                new Message()
                    .workflowInstanceId(instance.getId())
                    .activityInstanceId(waitingTask.getId())
                    .data("result", "任务完成")
            );

            System.out.println("\n=== 发送消息后状态 ===");
            printDetailedStatus(updatedInstance);
        }
    }

    private static ActivityInstance findWaitingActivity(WorkflowInstance instance, String activityId) {
        // 方法1: 使用便捷方法
        ActivityInstance found = instance.findOpenActivityInstance(activityId);
        if (found != null) {
            return found;
        }

        // 方法2: 手动遍历查找
        List<ActivityInstance> openActivities = instance.getOpenActivityInstances();
        for (ActivityInstance activity : openActivities) {
            if (activityId.equals(activity.getActivityId())) {
                return activity;
            }
        }

        return null;
    }

    private static void printDetailedStatus(WorkflowInstance instance) {
        System.out.println("工作流实例ID: " + instance.getId());
        System.out.println("工作流状态: " + (instance.isEnded() ? "已结束" : "进行中"));

        // 显示所有活动实例
        List<ActivityInstance> allActivities = instance.getActivityInstances();
        if (allActivities != null) {
            System.out.println("所有活动实例:");
            for (ActivityInstance activity : allActivities) {
                String status = activity.isEnded() ? "已完成" : "进行中";
                System.out.println("  - " + activity.getActivityId() +
                                 " (实例ID: " + activity.getId() +
                                 ", 状态: " + status + ")");
            }
        }

        // 显示开放的活动
        List<ActivityInstance> openActivities = instance.getOpenActivityInstances();
        if (!openActivities.isEmpty()) {
            System.out.println("等待中的活动:");
            for (ActivityInstance activity : openActivities) {
                System.out.println("  - " + activity.getActivityId() +
                                 " (实例ID: " + activity.getId() + ")");
            }
        }

        // 显示变量值
        if (instance.getVariableInstances() != null && !instance.getVariableInstances().isEmpty()) {
            System.out.println("变量值:");
            for (VariableInstance var : instance.getVariableInstances()) {
                System.out.println("  - " + var.getVariableId() + " = " + var.getValue());
            }
        }
    }
}
```

### 工作流执行监听器

```java
public class WorkflowExecutionListener implements com.effektif.workflow.impl.WorkflowExecutionListener {

    @Override
    public void starting(WorkflowInstanceImpl workflowInstance) {
        System.out.println("工作流实例启动: " + workflowInstance.getId());
    }

    @Override
    public void ended(WorkflowInstanceImpl workflowInstance) {
        System.out.println("工作流实例结束: " + workflowInstance.getId());
    }

    @Override
    public void activityInstanceStarted(ActivityInstanceImpl activityInstance) {
        System.out.println("活动开始: " + activityInstance.getActivity().getId() +
                          " (实例: " + activityInstance.getId() + ")");
    }

    @Override
    public void activityInstanceEnded(ActivityInstanceImpl activityInstance) {
        System.out.println("活动结束: " + activityInstance.getActivity().getId() +
                          " (实例: " + activityInstance.getId() + ")");
    }

    @Override
    public void transitioning(ActivityInstanceImpl from, TransitionImpl transition, ActivityInstanceImpl to) {
        String fromActivity = from != null ? from.getActivity().getId() : "null";
        String toActivity = to != null ? to.getActivity().getId() : "null";
        System.out.println("流转: " + fromActivity + " -> " + toActivity);
    }
}

// 使用监听器
Configuration configuration = new MemoryConfiguration();
configuration.ingredient(new WorkflowExecutionListener());
configuration.start();
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
