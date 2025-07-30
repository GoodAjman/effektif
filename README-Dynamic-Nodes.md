# Effektif 动态节点系统

## 概述

Effektif 动态节点系统是一个基于 Effektif 工作流引擎的扩展框架，提供了类似 n8n 和 Zapier 的动态节点功能。该系统支持灵活的节点配置、表达式解析、数据拆分和批量处理，能够轻松集成各种外部系统和服务。

## 核心特性

### 🎯 统一节点接口
- **DynamicNode**: 统一的节点抽象接口
- **TriggerNode**: 触发节点，支持轮询、Webhook、定时等策略
- **ActionNode**: 动作节点，执行具体的业务操作
- **TransformNode**: 转换节点，处理数据格式转换

### 🔀 智能数据拆分
- **自动检测**: 智能识别数组、对象等数据格式
- **批量处理**: 自动拆分批量数据为独立的工作流实例
- **错误隔离**: 单个数据项处理失败不影响其他数据项
- **元数据保留**: 保留数据索引、总数等元数据信息

### 📝 强大表达式引擎
- **多格式支持**: `{{variable}}`、`{{= expression}}`、`{{$json.path}}`
- **JavaScript 执行**: 支持复杂的 JavaScript 表达式
- **JSON 路径查询**: 使用 JSONPath 提取数据
- **变量引用**: 支持工作流变量和节点数据引用

### 🚀 无缝 Effektif 集成
- **适配器模式**: 通过适配器无缝集成到 Effektif 引擎
- **异步执行**: 支持异步节点执行和状态管理
- **错误处理**: 完善的错误处理和重试机制

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    动态节点系统架构                          │
├─────────────────────────────────────────────────────────────┤
│  🎯 节点层        │  🔧 执行层        │  💾 数据层        │
│  ┌─────────────┐  │  ┌─────────────┐  │  ┌─────────────┐  │
│  │ TriggerNode │  │  │ NodeExecutor│  │  │ DataSplitter│  │
│  │ ActionNode  │  │  │ DataPipeline│  │  │ DataMapper  │  │
│  │ TransformNode│  │  │ ContextMgr  │  │  │ ExprEngine  │  │
│  └─────────────┘  │  └─────────────┘  │  └─────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                🚀 Effektif 集成层                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ DynamicActivityType │ WorkflowEngine │ ActivityInstance │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 创建 HTTP 触发节点

```java
// 配置 HTTP 轮询触发器
NodeConfig config = NodeConfig.builder()
    .put("apiUrl", "https://api.example.com/orders")
    .put("pollInterval", 60)  // 每60秒轮询一次
    .put("method", "GET")
    .put("headers", Map.of("Authorization", "Bearer token123"))
    .put("dataPath", "$.data")  // 从响应的data字段提取数据
    .build();

// 创建并初始化触发器
HttpPollingTriggerNode trigger = new HttpPollingTriggerNode();
trigger.initialize(config);

// 启动触发器
trigger.startTrigger().thenRun(() -> {
    System.out.println("触发器启动成功");
});
```

### 2. 创建 HTTP 动作节点

```java
// 配置 HTTP 请求动作节点
NodeConfig config = NodeConfig.builder()
    .put("url", "https://api.example.com/users/{{$json.userId}}")
    .put("method", "POST")
    .put("headers", Map.of(
        "Content-Type", "application/json",
        "Authorization", "Bearer {{$workflow.apiToken}}"
    ))
    .put("body", Map.of(
        "name", "{{$json.name}}",
        "email", "{{$json.email}}",
        "timestamp", "{{= new Date().toISOString()}}"
    ))
    .build();

// 创建并执行动作
HttpRequestActionNode action = new HttpRequestActionNode();
action.initialize(config);

ActionContext context = ActionContext.builder()
    .inputData(Map.of("userId", "123", "name", "Alice"))
    .variable("apiToken", "abc123")
    .build();

ActionResult result = action.executeAction(context);
```

### 3. 集成到 Effektif 工作流

```java
// 创建动态节点
DynamicNode httpNode = new HttpRequestActionNode();

// 创建适配器
DynamicActivityType activityType = new DynamicActivityType(httpNode, config);

// 注册到工作流引擎
workflowEngine.registerActivityType(activityType);

// 在工作流定义中使用
WorkflowDefinition workflow = new WorkflowDefinition()
    .activity("httpRequest", new DynamicActivity()
        .nodeType("http_request")
        .config(config))
    .transition(from("httpRequest").to("nextActivity"));
```

## 表达式语法

### 变量引用
```javascript
{{userName}}           // 简单变量引用
{{$json.user.name}}    // JSON 路径引用
{{$workflow.apiKey}}   // 工作流变量引用
{{$node.previous.id}}  // 节点数据引用
```

### JavaScript 表达式
```javascript
{{= name.toUpperCase()}}                    // 字符串操作
{{= age >= 18 ? 'adult' : 'minor'}}        // 条件表达式
{{= new Date().toISOString()}}             // 日期函数
{{= Math.round(price * 1.2)}}              // 数学计算
```

### 复合表达式
```javascript
// 在字符串中混合使用
"Hello {{name}}, your order {{= orderId.toString().padStart(6, '0')}} is ready!"

// 在对象中使用
{
  "userId": "{{$json.id}}",
  "fullName": "{{= firstName + ' ' + lastName}}",
  "createdAt": "{{= new Date().toISOString()}}"
}
```

## 数据拆分策略

### 自动检测拆分
```java
// 输入数据：[{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]
List<DataItem> items = dataSplitter.splitData(data, SplitStrategy.AUTO_DETECT, null);
// 结果：2个独立的 DataItem，每个包含一个用户数据
```

### 自定义路径拆分
```java
// 输入数据：{"users": [...], "total": 10}
List<DataItem> items = dataSplitter.splitData(data, SplitStrategy.CUSTOM_PATH, "$.users");
// 结果：从 users 数组中拆分出的 DataItem 列表
```

### 强制数组拆分
```java
// 即使是单个对象也包装为数组处理
List<DataItem> items = dataSplitter.splitData(singleUser, SplitStrategy.FORCE_ARRAY, null);
// 结果：包含单个 DataItem 的列表
```

## 节点类型

### 触发节点 (TriggerNode)
- **HttpPollingTriggerNode**: HTTP API 轮询触发器
- **WebhookTriggerNode**: Webhook 接收触发器
- **ScheduledTriggerNode**: 定时任务触发器
- **FileTriggerNode**: 文件监听触发器

### 动作节点 (ActionNode)
- **HttpRequestActionNode**: HTTP 请求动作
- **DatabaseActionNode**: 数据库操作动作
- **EmailActionNode**: 邮件发送动作
- **FileActionNode**: 文件处理动作

### 转换节点 (TransformNode)
- **DataMappingNode**: 数据字段映射
- **FormatConversionNode**: 格式转换
- **FilterNode**: 数据过滤
- **AggregationNode**: 数据聚合

## 配置参数

### 通用配置
```yaml
nodeType: "http_request"           # 节点类型
name: "调用用户API"                # 节点名称
description: "获取用户详细信息"     # 节点描述
timeout: 30000                    # 超时时间(毫秒)
retryCount: 3                     # 重试次数
retryDelay: 1000                  # 重试延迟(毫秒)
```

### HTTP 触发器配置
```yaml
apiUrl: "https://api.example.com/data"
pollInterval: 60                  # 轮询间隔(秒)
method: "GET"                     # HTTP方法
headers:                          # 请求头
  Authorization: "Bearer {{token}}"
queryParams:                      # 查询参数
  status: "active"
dataPath: "$.data"               # 数据提取路径
lastPollTimeField: "updated_at"  # 增量拉取字段
```

### HTTP 动作配置
```yaml
url: "https://api.example.com/users/{{$json.id}}"
method: "POST"
headers:
  Content-Type: "application/json"
  Authorization: "Bearer {{$workflow.token}}"
body:
  name: "{{$json.name}}"
  email: "{{$json.email}}"
  timestamp: "{{= new Date().toISOString()}}"
```

## 监控和调试

### 执行统计
```java
// 获取触发器统计信息
TriggerStatistics stats = triggerNode.getStatistics();
System.out.println("轮询次数: " + stats.getPollCount());
System.out.println("成功次数: " + stats.getSuccessCount());
System.out.println("错误次数: " + stats.getErrorCount());

// 获取节点状态
NodeStatus status = dynamicNode.getStatus();
System.out.println("节点状态: " + status);
```

### 健康检查
```java
// 检查触发器健康状态
HealthCheckResult health = triggerNode.checkHealth();
if (health.isHealthy()) {
    System.out.println("触发器运行正常");
} else {
    System.out.println("触发器异常: " + health.getMessage());
}
```

### 表达式缓存
```java
// 获取表达式引擎缓存统计
ExpressionEngine.CacheStatistics cacheStats = expressionEngine.getCacheStatistics();
System.out.println("缓存使用率: " + cacheStats.getUsageRatio());

// 清理缓存
expressionEngine.clearCache();
```

## 扩展开发

### 自定义触发节点
```java
@Component
public class CustomTriggerNode implements TriggerNode {
    
    @Override
    public TriggerStrategy getTriggerStrategy() {
        return TriggerStrategy.EVENT_DRIVEN;
    }
    
    @Override
    public CompletableFuture<Void> startTrigger() {
        // 实现自定义触发逻辑
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public List<WorkflowExecution> processTriggerData(Object data, TriggerContext context) {
        // 实现数据处理逻辑
        return Collections.emptyList();
    }
    
    // 实现其他必要方法...
}
```

### 自定义动作节点
```java
@Component
public class CustomActionNode implements ActionNode {
    
    @Override
    public ActionType getActionType() {
        return ActionType.CUSTOM;
    }
    
    @Override
    public ActionResult executeAction(ActionContext context) {
        try {
            // 实现自定义业务逻辑
            Object result = performCustomOperation(context);
            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }
    
    // 实现其他必要方法...
}
```

## 最佳实践

### 1. 错误处理
- 使用适当的重试策略
- 实现优雅的错误降级
- 记录详细的错误日志

### 2. 性能优化
- 合理设置轮询间隔
- 使用批量操作提高效率
- 缓存频繁使用的表达式

### 3. 安全考虑
- 验证输入数据格式
- 使用安全的表达式执行环境
- 保护敏感配置信息

### 4. 监控运维
- 设置适当的超时时间
- 监控节点执行状态
- 定期检查系统健康状态

## 版本历史

- **v2.0**: 完整的动态节点系统，支持表达式引擎和数据拆分
- **v1.5**: 添加批量处理和错误隔离功能
- **v1.0**: 基础的触发器和动作节点实现

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进这个项目。在提交代码前，请确保：

1. 代码符合项目的编码规范
2. 添加了适当的单元测试
3. 更新了相关文档
4. 通过了所有测试用例

## 许可证

本项目采用 Apache License 2.0 许可证。详情请参见 [LICENSE](LICENSE) 文件。
