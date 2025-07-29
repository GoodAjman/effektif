# Effektif Integration Platform 2.0

基于Spring Boot的企业级工作流集成平台，提供多种触发器支持和完整的工作流管理功能。

## 📚 目录

- [功能特性](#-功能特性)
- [触发器类型](#触发器类型)
- [Effektif常见BPMN节点](#-effektif常见bpmn节点)
  - [基础节点类型](#基础节点类型)
  - [网关节点](#网关节点)
  - [中间事件](#中间事件)
  - [子流程](#子流程)
  - [边界事件](#边界事件)
  - [多实例活动](#多实例活动)
  - [完整工作流示例](#完整工作流示例)
  - [高级节点特性](#高级节点特性)
  - [动态节点配置](#动态节点配置)
  - [流程监控和管理](#流程监控和管理)
- [架构设计](#️-架构设计)
- [快速开始](#-快速开始)
- [使用指南](#-使用指南)
- [配置说明](#-配置说明)
- [测试](#-测试)
- [监控和运维](#-监控和运维)
- [安全考虑](#-安全考虑)
- [部署指南](#-部署指南)
- [贡献指南](#-贡献指南)

## 🚀 功能特性

### 核心功能
- **多种触发器支持**: HTTP Webhook、消息队列、定时任务、数据库变更、文件监听
- **工作流引擎集成**: 基于Effektif工作流引擎，支持BPMN 2.0标准
- **企业级特性**: 高可用、集群支持、监控告警、安全认证
- **RESTful API**: 完整的REST API接口，支持OpenAPI 3.0文档
- **可视化管理**: 触发器配置管理、执行日志查询、性能监控

### 触发器类型

#### 1. HTTP触发器 (HttpTrigger)
- 支持多种HTTP方法 (GET/POST/PUT/DELETE等)
- 支持多种数据格式 (JSON/XML/Form等)
- 签名验证和IP白名单控制
- 异步处理支持

#### 2. 消息队列触发器 (MessageQueueTrigger)
- 支持RabbitMQ、Kafka、ActiveMQ等
- 消息确认机制和死信队列处理
- 批量消息处理和消息去重
- 重试机制和错误处理

#### 3. 定时触发器 (ScheduledTrigger)
- 支持Cron表达式和固定间隔
- 时区处理和错过执行策略
- 任务持久化和集群部署
- 并发控制和超时处理

#### 4. 数据库触发器 (DatabaseTrigger)
- 支持MySQL、PostgreSQL、Oracle等
- CDC(Change Data Capture)技术
- 表级和行级过滤
- 事务一致性保证

#### 5. 文件触发器 (FileTrigger)
- 文件系统变更监听
- 支持FTP/SFTP远程文件
- 文件过滤规则
- 目录递归监听

## 🔧 Effektif常见BPMN节点

### 基础节点类型

#### 1. 开始事件 (StartEvent)
```java
// 基础开始事件
StartEvent startEvent = new StartEvent()
    .id("start")
    .name("流程开始");

// 定时开始事件
TimerStartEvent timerStart = new TimerStartEvent()
    .id("timerStart")
    .name("定时开始")
    .timer(new Timer().cron("0 0 9 * * ?"));

// 消息开始事件
MessageStartEvent messageStart = new MessageStartEvent()
    .id("messageStart")
    .name("消息开始")
    .message("orderCreated");
```

#### 2. 结束事件 (EndEvent)
```java
// 基础结束事件
EndEvent endEvent = new EndEvent()
    .id("end")
    .name("流程结束");

// 错误结束事件
ErrorEndEvent errorEnd = new ErrorEndEvent()
    .id("errorEnd")
    .name("错误结束")
    .errorCode("BUSINESS_ERROR")
    .errorMessage("业务处理失败");

// 终止结束事件
TerminateEndEvent terminateEnd = new TerminateEndEvent()
    .id("terminateEnd")
    .name("终止流程");
```

#### 3. 用户任务 (UserTask)
```java
UserTask userTask = new UserTask()
    .id("approveTask")
    .name("审批任务")
    .assignee("${manager}")
    .candidateGroups("approvers")
    .dueDate("${now().plusDays(3)}")
    .form(new Form()
        .field(new TextField("comment").label("审批意见"))
        .field(new ChoiceField("decision")
            .label("审批结果")
            .option("approve", "同意")
            .option("reject", "拒绝")));
```

#### 4. 服务任务 (ServiceTask)
```java
ServiceTask serviceTask = new ServiceTask()
    .id("sendEmail")
    .name("发送邮件")
    .serviceClass("com.example.EmailService")
    .property("to", "${applicant.email}")
    .property("subject", "申请结果通知")
    .property("template", "approval-result");

// Java委托服务任务
JavaServiceTask javaTask = new JavaServiceTask()
    .id("calculateAmount")
    .name("计算金额")
    .javaClass("com.example.AmountCalculator")
    .inputParameter("baseAmount", "${order.amount}")
    .outputParameter("totalAmount", "${result.total}");
```

### 网关节点

#### 1. 排他网关 (ExclusiveGateway)
```java
ExclusiveGateway exclusiveGateway = new ExclusiveGateway()
    .id("decision")
    .name("审批决策");

// 排他网关的条件分支
SequenceFlow approveFlow = new SequenceFlow()
    .id("approveFlow")
    .from("decision")
    .to("approved")
    .condition("${decision == 'approve'}");

SequenceFlow rejectFlow = new SequenceFlow()
    .id("rejectFlow")
    .from("decision")
    .to("rejected")
    .condition("${decision == 'reject'}");

// 默认分支
SequenceFlow defaultFlow = new SequenceFlow()
    .id("defaultFlow")
    .from("decision")
    .to("review")
    .isDefault(true);
```

#### 2. 并行网关 (ParallelGateway)
```java
// 并行分支网关
ParallelGateway parallelSplit = new ParallelGateway()
    .id("parallelSplit")
    .name("并行处理");

// 并行汇聚网关
ParallelGateway parallelJoin = new ParallelGateway()
    .id("parallelJoin")
    .name("等待汇聚");

// 并行分支示例
SequenceFlow branch1 = new SequenceFlow()
    .from("parallelSplit")
    .to("task1");

SequenceFlow branch2 = new SequenceFlow()
    .from("parallelSplit")
    .to("task2");

SequenceFlow branch3 = new SequenceFlow()
    .from("parallelSplit")
    .to("task3");
```

#### 3. 包容网关 (InclusiveGateway)
```java
InclusiveGateway inclusiveGateway = new InclusiveGateway()
    .id("inclusiveDecision")
    .name("包容决策");

// 多条件分支，满足条件的都会执行
SequenceFlow emailFlow = new SequenceFlow()
    .from("inclusiveDecision")
    .to("sendEmail")
    .condition("${sendEmail == true}");

SequenceFlow smsFlow = new SequenceFlow()
    .from("inclusiveDecision")
    .to("sendSMS")
    .condition("${sendSMS == true}");

SequenceFlow pushFlow = new SequenceFlow()
    .from("inclusiveDecision")
    .to("sendPush")
    .condition("${sendPush == true}");
```

#### 4. 基于事件的网关 (EventBasedGateway)
```java
EventBasedGateway eventGateway = new EventBasedGateway()
    .id("eventDecision")
    .name("事件决策");

// 定时器中间事件
TimerIntermediateCatchEvent timerEvent = new TimerIntermediateCatchEvent()
    .id("timeout")
    .name("超时等待")
    .timer(new Timer().duration("PT1H")); // 1小时

// 消息中间事件
MessageIntermediateCatchEvent messageEvent = new MessageIntermediateCatchEvent()
    .id("response")
    .name("等待响应")
    .message("userResponse");
```

### 中间事件

#### 1. 定时器中间事件
```java
// 定时器捕获事件
TimerIntermediateCatchEvent timerCatch = new TimerIntermediateCatchEvent()
    .id("wait")
    .name("等待处理")
    .timer(new Timer()
        .duration("PT30M")  // 30分钟
        .cycle("R3/PT1H"));  // 重复3次，每小时一次

// 定时器抛出事件
TimerIntermediateThrowEvent timerThrow = new TimerIntermediateThrowEvent()
    .id("delay")
    .name("延迟处理")
    .timer(new Timer().duration("PT5M"));
```

#### 2. 消息中间事件
```java
// 消息捕获事件
MessageIntermediateCatchEvent messageCatch = new MessageIntermediateCatchEvent()
    .id("waitForApproval")
    .name("等待审批")
    .message("approvalResponse");

// 消息抛出事件
MessageIntermediateThrowEvent messageThrow = new MessageIntermediateThrowEvent()
    .id("notifyManager")
    .name("通知经理")
    .message("managerNotification")
    .property("recipient", "${manager.email}")
    .property("content", "${notification.message}");
```

#### 3. 信号中间事件
```java
// 信号捕获事件
SignalIntermediateCatchEvent signalCatch = new SignalIntermediateCatchEvent()
    .id("waitSignal")
    .name("等待信号")
    .signal("processComplete");

// 信号抛出事件
SignalIntermediateThrowEvent signalThrow = new SignalIntermediateThrowEvent()
    .id("broadcastSignal")
    .name("广播信号")
    .signal("dataUpdated")
    .scope(SignalScope.GLOBAL);
```

### 子流程

#### 1. 嵌入式子流程 (SubProcess)
```java
SubProcess subProcess = new SubProcess()
    .id("handleException")
    .name("异常处理子流程")
    .triggeredByEvent(false)
    .activity(new StartEvent().id("subStart"))
    .activity(new UserTask()
        .id("handleError")
        .name("处理异常")
        .assignee("${errorHandler}"))
    .activity(new EndEvent().id("subEnd"));
```

#### 2. 调用活动 (CallActivity)
```java
CallActivity callActivity = new CallActivity()
    .id("callSubProcess")
    .name("调用子流程")
    .calledElement("commonApprovalProcess")
    .inputParameter("applicant", "${currentUser}")
    .inputParameter("amount", "${requestAmount}")
    .outputParameter("result", "${approvalResult}");
```

#### 3. 事件子流程 (EventSubProcess)
```java
EventSubProcess eventSubProcess = new EventSubProcess()
    .id("errorHandler")
    .name("错误处理子流程")
    .triggeredByEvent(true)
    .activity(new ErrorStartEvent()
        .id("errorStart")
        .errorCode("BUSINESS_ERROR"))
    .activity(new ServiceTask()
        .id("logError")
        .name("记录错误")
        .serviceClass("com.example.ErrorLogger"))
    .activity(new EndEvent().id("errorEnd"));
```

### 边界事件

#### 1. 定时器边界事件
```java
UserTask longRunningTask = new UserTask()
    .id("complexApproval")
    .name("复杂审批")
    .assignee("${approver}");

// 附加到任务的定时器边界事件
TimerBoundaryEvent timerBoundary = new TimerBoundaryEvent()
    .id("approvalTimeout")
    .name("审批超时")
    .attachedTo("complexApproval")
    .cancelActivity(true)  // 取消原活动
    .timer(new Timer().duration("P3D"));  // 3天
```

#### 2. 错误边界事件
```java
ServiceTask riskyTask = new ServiceTask()
    .id("externalCall")
    .name("外部服务调用")
    .serviceClass("com.example.ExternalService");

ErrorBoundaryEvent errorBoundary = new ErrorBoundaryEvent()
    .id("handleError")
    .name("处理错误")
    .attachedTo("externalCall")
    .errorCode("CONNECTION_ERROR")
    .cancelActivity(true);
```

#### 3. 消息边界事件
```java
UserTask waitingTask = new UserTask()
    .id("waitForInput")
    .name("等待用户输入")
    .assignee("${user}");

MessageBoundaryEvent messageBoundary = new MessageBoundaryEvent()
    .id("cancelMessage")
    .name("取消消息")
    .attachedTo("waitForInput")
    .message("cancelRequest")
    .cancelActivity(true);
```

### 多实例活动

#### 1. 并行多实例
```java
UserTask parallelReview = new UserTask()
    .id("parallelReview")
    .name("并行审核")
    .assignee("${reviewer}")
    .multiInstance(new MultiInstance()
        .parallel(true)
        .collection("${reviewers}")
        .elementVariable("reviewer")
        .completionCondition("${nrOfCompletedInstances >= 2}"));  // 至少2人完成
```

#### 2. 顺序多实例
```java
ServiceTask sequentialProcess = new ServiceTask()
    .id("sequentialProcess")
    .name("顺序处理")
    .serviceClass("com.example.DataProcessor")
    .multiInstance(new MultiInstance()
        .sequential(true)
        .collection("${dataList}")
        .elementVariable("dataItem")
        .completionCondition("${nrOfActiveInstances == 0}"));
```

### 完整工作流示例

```java
public class OrderApprovalWorkflow {

    public static Workflow createWorkflow() {
        return new Workflow()
            .id("orderApproval")
            .name("订单审批流程")
            .description("处理订单审批的完整流程")

            // 变量定义
            .variable("order", new ObjectType())
            .variable("approver", new TextType())
            .variable("decision", new TextType())
            .variable("amount", new NumberType())

            // 开始事件
            .activity(new StartEvent()
                .id("start")
                .name("开始"))

            // 自动任务：验证订单
            .activity(new ServiceTask()
                .id("validateOrder")
                .name("验证订单")
                .serviceClass("com.example.OrderValidator")
                .inputParameter("order", "${order}")
                .outputParameter("validationResult", "${result}"))

            // 排他网关：根据金额决定审批路径
            .activity(new ExclusiveGateway()
                .id("amountDecision")
                .name("金额决策"))

            // 小额订单直接通过
            .activity(new ServiceTask()
                .id("autoApprove")
                .name("自动审批")
                .serviceClass("com.example.AutoApprover"))

            // 大额订单需要人工审批
            .activity(new UserTask()
                .id("manualApproval")
                .name("人工审批")
                .assignee("${approver}")
                .form(new Form()
                    .field(new TextField("comment").label("审批意见"))
                    .field(new ChoiceField("decision")
                        .option("approve", "同意")
                        .option("reject", "拒绝"))))

            // 审批超时边界事件
            .activity(new TimerBoundaryEvent()
                .id("approvalTimeout")
                .name("审批超时")
                .attachedTo("manualApproval")
                .timer(new Timer().duration("P2D"))
                .cancelActivity(false))

            // 超时处理
            .activity(new ServiceTask()
                .id("handleTimeout")
                .name("处理超时")
                .serviceClass("com.example.TimeoutHandler"))

            // 审批结果网关
            .activity(new ExclusiveGateway()
                .id("approvalResult")
                .name("审批结果"))

            // 并行处理：通知和记录
            .activity(new ParallelGateway()
                .id("parallelProcess")
                .name("并行处理"))

            // 发送通知
            .activity(new ServiceTask()
                .id("sendNotification")
                .name("发送通知")
                .serviceClass("com.example.NotificationService"))

            // 记录日志
            .activity(new ServiceTask()
                .id("logResult")
                .name("记录结果")
                .serviceClass("com.example.AuditLogger"))

            // 汇聚网关
            .activity(new ParallelGateway()
                .id("joinGateway")
                .name("汇聚"))

            // 结束事件
            .activity(new EndEvent()
                .id("end")
                .name("结束"))

            // 流程连接
            .transition(transition().from("start").to("validateOrder"))
            .transition(transition().from("validateOrder").to("amountDecision"))
            .transition(transition().from("amountDecision").to("autoApprove")
                .condition("${amount < 1000}"))
            .transition(transition().from("amountDecision").to("manualApproval")
                .condition("${amount >= 1000}"))
            .transition(transition().from("autoApprove").to("parallelProcess"))
            .transition(transition().from("manualApproval").to("approvalResult"))
            .transition(transition().from("approvalTimeout").to("handleTimeout"))
            .transition(transition().from("handleTimeout").to("approvalResult"))
            .transition(transition().from("approvalResult").to("parallelProcess")
                .condition("${decision == 'approve'}"))
            .transition(transition().from("approvalResult").to("end")
                .condition("${decision == 'reject'}"))
            .transition(transition().from("parallelProcess").to("sendNotification"))
            .transition(transition().from("parallelProcess").to("logResult"))
            .transition(transition().from("sendNotification").to("joinGateway"))
            .transition(transition().from("logResult").to("joinGateway"))
            .transition(transition().from("joinGateway").to("end"));
    }
}
```

### 高级节点特性

#### 1. 条件表达式
```java
// 支持多种条件表达式
SequenceFlow conditionalFlow = new SequenceFlow()
    .condition("${amount > 1000 && department == 'finance'}")
    .condition("${user.hasRole('manager')}")
    .condition("${order.status == 'pending' && order.priority == 'high'}");

// 复杂条件表达式
ExclusiveGateway complexGateway = new ExclusiveGateway()
    .id("complexDecision");

// 使用脚本条件
SequenceFlow scriptFlow = new SequenceFlow()
    .condition(new Script()
        .language("javascript")
        .script("order.amount > 1000 && order.customer.vipLevel >= 3"));
```

#### 2. 数据对象和数据存储
```java
// 数据对象定义
DataObject orderData = new DataObject()
    .id("orderDataObj")
    .name("订单数据")
    .type(new ObjectType());

// 数据存储
DataStore customerDB = new DataStore()
    .id("customerDatabase")
    .name("客户数据库")
    .capacity(1000);

// 数据输入关联
DataInputAssociation dataInput = new DataInputAssociation()
    .sourceRef("orderDataObj")
    .targetRef("processOrder.orderInput");
```

#### 3. 补偿处理
```java
// 补偿任务
ServiceTask compensationTask = new ServiceTask()
    .id("rollbackPayment")
    .name("回滚支付")
    .serviceClass("com.example.PaymentRollback")
    .isForCompensation(true);

// 补偿边界事件
CompensationBoundaryEvent compensationBoundary = new CompensationBoundaryEvent()
    .id("compensate")
    .name("补偿处理")
    .attachedTo("processPayment");

// 补偿抛出事件
CompensationIntermediateThrowEvent compensationThrow = new CompensationIntermediateThrowEvent()
    .id("triggerCompensation")
    .name("触发补偿")
    .activityRef("processPayment");
```

#### 4. 升级处理
```java
// 升级边界事件
EscalationBoundaryEvent escalationBoundary = new EscalationBoundaryEvent()
    .id("escalateToManager")
    .name("升级到经理")
    .attachedTo("handleComplaint")
    .escalationCode("CUSTOMER_COMPLAINT")
    .cancelActivity(false);

// 升级结束事件
EscalationEndEvent escalationEnd = new EscalationEndEvent()
    .id("escalateEnd")
    .name("升级结束")
    .escalationCode("UNRESOLVED_ISSUE");
```

#### 5. 链接事件
```java
// 链接抛出事件
LinkIntermediateThrowEvent linkThrow = new LinkIntermediateThrowEvent()
    .id("linkToNextPhase")
    .name("链接到下一阶段")
    .linkName("nextPhase");

// 链接捕获事件
LinkIntermediateCatchEvent linkCatch = new LinkIntermediateCatchEvent()
    .id("continueFromLink")
    .name("从链接继续")
    .linkName("nextPhase");
```

### 动态节点配置

#### 1. 动态任务分配
```java
UserTask dynamicTask = new UserTask()
    .id("dynamicAssignment")
    .name("动态分配任务")
    .assignee("${assignmentService.getAssignee(order.department)}")
    .candidateGroups("${roleService.getRoles(order.type)}")
    .dueDate("${dueDateCalculator.calculate(order.priority)}")
    .priority("${priorityCalculator.calculate(order.urgency)}");
```

#### 2. 动态表单生成
```java
UserTask dynamicForm = new UserTask()
    .id("dynamicFormTask")
    .name("动态表单任务")
    .form(new DynamicForm()
        .formKey("${formService.getFormKey(processType)}")
        .fields("${formService.getFields(processType)}"));
```

#### 3. 动态服务调用
```java
ServiceTask dynamicService = new ServiceTask()
    .id("dynamicService")
    .name("动态服务调用")
    .serviceClass("${serviceRegistry.getService(serviceType)}")
    .method("${methodResolver.getMethod(operation)}")
    .parameters("${parameterBuilder.build(context)}");
```

#### 4. 条件化活动
```java
// 条件化任务
UserTask conditionalTask = new UserTask()
    .id("conditionalApproval")
    .name("条件审批")
    .condition("${needsApproval == true}")
    .assignee("${approver}")
    .skipExpression("${autoApprove == true}");

// 条件化子流程
SubProcess conditionalSubProcess = new SubProcess()
    .id("conditionalHandling")
    .name("条件处理")
    .condition("${complexCase == true}")
    .activity(new StartEvent().id("subStart"))
    .activity(new UserTask().id("complexHandling"))
    .activity(new EndEvent().id("subEnd"));
```

### 流程监控和管理

#### 1. 流程变量监听
```java
// 变量监听器
VariableListener variableListener = new VariableListener()
    .variableName("orderStatus")
    .event(VariableEvent.CREATE, VariableEvent.UPDATE)
    .listenerClass("com.example.OrderStatusListener");

// 执行监听器
ExecutionListener executionListener = new ExecutionListener()
    .event(ExecutionEvent.START, ExecutionEvent.END)
    .listenerClass("com.example.ProcessExecutionListener");
```

#### 2. 任务监听器
```java
UserTask monitoredTask = new UserTask()
    .id("monitoredTask")
    .name("监控任务")
    .taskListener(new TaskListener()
        .event(TaskEvent.CREATE)
        .listenerClass("com.example.TaskCreateListener"))
    .taskListener(new TaskListener()
        .event(TaskEvent.COMPLETE)
        .listenerClass("com.example.TaskCompleteListener"));
```

#### 3. 流程实例管理
```java
// 流程实例查询
ProcessInstanceQuery query = workflowEngine
    .getProcessInstanceQuery()
    .processDefinitionKey("orderApproval")
    .variableValueEquals("department", "finance")
    .active()
    .orderByStartTime()
    .desc();

// 流程实例操作
workflowEngine.suspendProcessInstance(processInstanceId);
workflowEngine.activateProcessInstance(processInstanceId);
workflowEngine.deleteProcessInstance(processInstanceId, "cancelled by user");
```

这些节点类型和配置示例展示了effektif工作流引擎的强大功能，支持复杂的业务流程建模和执行。通过Integration Platform 2.0，您可以轻松地创建、管理和监控这些工作流程。

## 🏗️ 架构设计

### 系统架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   外部事件源    │    │   Trigger层     │    │  工作流引擎     │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ HTTP请求        │───▶│ HttpTrigger     │───▶│ TriggerInstance │
│ 消息队列        │───▶│ MQTrigger       │───▶│ WorkflowEngine  │
│ 定时任务        │───▶│ ScheduledTrigger│───▶│ WorkflowInstance│
│ 数据库变更      │───▶│ DatabaseTrigger │───▶│                 │
│ 文件变更        │───▶│ FileTrigger     │───▶│                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                ▲
                                │
                    ┌─────────────────┐
                    │ Spring Boot     │
                    │ 集成平台        │
                    ├─────────────────┤
                    │ REST API        │
                    │ 配置管理        │
                    │ 监控告警        │
                    │ 安全认证        │
                    └─────────────────┘
```

### 技术栈
- **框架**: Spring Boot 2.7.x
- **工作流引擎**: Effektif
- **数据库**: MySQL 8.0 / H2 (开发环境)
- **消息队列**: RabbitMQ
- **调度器**: Quartz
- **缓存**: Redis (可选)
- **监控**: Micrometer + Prometheus
- **文档**: OpenAPI 3.0 (Swagger)

## 🚀 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 8.0+ (生产环境)
- RabbitMQ 3.8+ (可选)
- MongoDB 4.0+ (Effektif工作流引擎)

### 1. 克隆项目
```bash
git clone https://github.com/GoodAjman/effektif.git
cd effektif/effektif-integration-platform
```

### 2. 配置数据库
```sql
-- 创建数据库
CREATE DATABASE effektif_integration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'effektif'@'localhost' IDENTIFIED BY 'effektif123';
GRANT ALL PRIVILEGES ON effektif_integration.* TO 'effektif'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 修改配置文件
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/effektif_integration
    username: effektif
    password: effektif123
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mongodb:
  host: localhost
  port: 27017
  database: effektif
```

### 4. 启动应用
```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=development

# 生产环境
mvn clean package
java -jar target/effektif-integration-platform-2.0.0.jar --spring.profiles.active=production
```

### 5. 访问应用
- **应用地址**: http://localhost:8080/api
- **API文档**: http://localhost:8080/api/swagger-ui.html
- **健康检查**: http://localhost:8080/api/actuator/health
- **指标监控**: http://localhost:8080/api/actuator/metrics

## 📖 使用指南

### 创建HTTP触发器
```bash
curl -X POST http://localhost:8080/api/triggers \
  -H "Content-Type: application/json" \
  -d '{
    "triggerId": "payment-webhook-001",
    "triggerType": "httpTrigger",
    "workflowId": "payment-process-workflow",
    "name": "支付回调触发器",
    "description": "处理第三方支付平台的回调通知",
    "config": {
      "url": "/webhooks/payment",
      "method": "POST",
      "secretKey": "your-secret-key",
      "contentType": "application/json",
      "async": true,
      "enableSignatureVerification": true
    },
    "status": 1
  }'
```

### 触发HTTP Webhook
```bash
curl -X POST http://localhost:8080/api/triggers/http/payment-webhook-001 \
  -H "Content-Type: application/json" \
  -H "X-Signature: sha256=your-signature" \
  -d '{
    "orderId": "ORDER-20240101-001",
    "amount": 299.99,
    "currency": "CNY",
    "status": "success"
  }'
```

### 创建定时触发器
```bash
curl -X POST http://localhost:8080/api/triggers \
  -H "Content-Type: application/json" \
  -d '{
    "triggerId": "daily-sync-001",
    "triggerType": "scheduledTrigger",
    "workflowId": "daily-data-sync-workflow",
    "name": "每日数据同步触发器",
    "config": {
      "cronExpression": "0 0 2 * * ?",
      "timeZone": "Asia/Shanghai",
      "persistent": true,
      "misfirePolicy": "DO_NOTHING"
    },
    "status": 1
  }'
```

### 查询触发器列表
```bash
curl -X GET "http://localhost:8080/api/triggers?triggerType=httpTrigger&status=1&page=0&size=20"
```

### 查看执行日志
```bash
curl -X GET "http://localhost:8080/api/triggers/payment-webhook-001/logs?page=0&size=20"
```

## 🔧 配置说明

### 应用配置
主要配置项说明：

```yaml
effektif:
  integration:
    platform:
      triggers:
        http:
          enabled: true                    # 是否启用HTTP触发器
          base-path: "/webhooks"           # HTTP触发器基础路径
          max-payload-size: 10485760       # 最大请求体大小(10MB)
          request-timeout: 30              # 请求超时时间(秒)
        
        message-queue:
          enabled: true                    # 是否启用消息队列触发器
          default-concurrency: 5           # 默认并发消费者数量
          max-retries: 3                   # 最大重试次数
          retry-interval: 5000             # 重试间隔(毫秒)
        
        scheduled:
          enabled: true                    # 是否启用定时触发器
          thread-pool-size: 10             # 线程池大小
          persistent-enabled: true         # 是否启用任务持久化
          cluster-enabled: true            # 是否启用集群模式
      
      security:
        enabled: true                      # 是否启用安全认证
        jwt-secret: "your-jwt-secret"      # JWT密钥
        token-expiration: 86400            # Token过期时间(秒)
      
      monitoring:
        enabled: true                      # 是否启用监控
        metrics-enabled: true              # 是否启用指标收集
        health-check-enabled: true         # 是否启用健康检查
```

### 环境变量
生产环境可通过环境变量覆盖配置：

```bash
export DATABASE_URL=jdbc:mysql://prod-db:3306/effektif_integration
export DATABASE_USERNAME=effektif_prod
export DATABASE_PASSWORD=your-secure-password
export RABBITMQ_HOST=prod-rabbitmq
export RABBITMQ_USERNAME=effektif
export RABBITMQ_PASSWORD=your-rabbitmq-password
export MONGODB_HOST=prod-mongodb
export MONGODB_DATABASE=effektif_prod
export SERVER_PORT=8080
```

## 🧪 测试

### 运行单元测试
```bash
mvn test
```

### 运行集成测试
```bash
mvn verify -P integration-test
```

### 压力测试
使用JMeter或其他工具进行压力测试：

```bash
# HTTP触发器并发测试
jmeter -n -t tests/http-trigger-load-test.jmx -l results/http-trigger-results.jtl

# 定时触发器稳定性测试
jmeter -n -t tests/scheduled-trigger-stability-test.jmx -l results/scheduled-trigger-results.jtl
```

## 📊 监控和运维

### 健康检查
```bash
curl http://localhost:8080/api/actuator/health
```

### 指标监控
```bash
# 查看所有指标
curl http://localhost:8080/api/actuator/metrics

# 查看特定指标
curl http://localhost:8080/api/actuator/metrics/trigger.execution.count
curl http://localhost:8080/api/actuator/metrics/trigger.execution.duration
```

### Prometheus集成
```bash
# Prometheus指标端点
curl http://localhost:8080/api/actuator/prometheus
```

### 日志管理
日志文件位置：
- 开发环境: `logs/effektif-integration-platform.log`
- 生产环境: `/var/log/effektif/integration-platform.log`

日志级别配置：
```yaml
logging:
  level:
    com.effektif: INFO
    org.springframework.amqp: WARN
```

## 🔒 安全考虑

### 认证和授权
- 支持JWT Token认证
- 基于角色的访问控制(RBAC)
- API密钥管理

### 数据安全
- 敏感配置信息加密存储
- 数据库连接加密
- 审计日志记录

### 网络安全
- HTTPS支持
- IP白名单控制
- 请求签名验证

## 🚀 部署指南

### Docker部署
```dockerfile
FROM openjdk:8-jre-alpine
COPY target/effektif-integration-platform-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 构建镜像
docker build -t effektif-integration-platform:2.0.0 .

# 运行容器
docker run -d \
  --name effektif-integration-platform \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DATABASE_URL=jdbc:mysql://mysql:3306/effektif_integration \
  effektif-integration-platform:2.0.0
```

### Kubernetes部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: effektif-integration-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: effektif-integration-platform
  template:
    metadata:
      labels:
        app: effektif-integration-platform
    spec:
      containers:
      - name: app
        image: effektif-integration-platform:2.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详情请参阅 [LICENSE](../LICENSE.md) 文件。

## 📞 支持

- 📧 邮箱: support@effektif.com
- 📖 文档: https://docs.effektif.com
- 🐛 问题反馈: https://github.com/GoodAjman/effektif/issues
- 💬 社区讨论: https://community.effektif.com
