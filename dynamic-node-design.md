# 动态节点设计方案

## 1. 核心接口设计

### 1.1 DynamicNode 基础接口

```java
/**
 * 动态节点基础接口
 * 参考 n8n 和 Zapier 的设计理念
 */
public interface DynamicNode {
    
    /**
     * 节点类型枚举
     */
    enum NodeType {
        TRIGGER,    // 触发节点
        ACTION,     // 动作节点
        TRANSFORM,  // 转换节点
        CONDITION,  // 条件节点
        WEBHOOK,    // Webhook节点
        HTTP,       // HTTP请求节点
        DATABASE,   // 数据库节点
        FILE        // 文件处理节点
    }
    
    /**
     * 数据处理策略
     */
    enum DataStrategy {
        SINGLE_ITEM,    // 单条数据处理
        BATCH_PROCESS,  // 批量处理
        STREAM_PROCESS  // 流式处理
    }
    
    /**
     * 获取节点类型
     */
    NodeType getNodeType();
    
    /**
     * 获取节点描述符（用于UI配置）
     */
    NodeDescriptor getDescriptor();
    
    /**
     * 初始化节点
     */
    void initialize(NodeConfig config);
    
    /**
     * 执行节点逻辑
     */
    CompletableFuture<NodeResult> execute(NodeContext context);
    
    /**
     * 验证节点配置
     */
    ValidationResult validate(NodeConfig config);
    
    /**
     * 获取支持的数据策略
     */
    List<DataStrategy> getSupportedDataStrategies();
}
```

### 1.2 TriggerNode 触发节点接口

```java
/**
 * 触发节点接口
 * 负责从外部系统拉取或接收数据
 */
public interface TriggerNode extends DynamicNode {
    
    /**
     * 触发策略枚举
     */
    enum TriggerStrategy {
        POLLING,     // 轮询拉取
        WEBHOOK,     // Webhook推送
        SCHEDULED,   // 定时触发
        EVENT_DRIVEN // 事件驱动
    }
    
    /**
     * 开始监听/拉取数据
     */
    CompletableFuture<Void> startTrigger();
    
    /**
     * 停止监听/拉取数据
     */
    void stopTrigger();
    
    /**
     * 获取触发策略
     */
    TriggerStrategy getTriggerStrategy();
    
    /**
     * 处理触发的数据
     * 支持数据拆分和批量处理
     */
    List<WorkflowExecution> processTriggerData(Object data, TriggerContext context);
    
    /**
     * 获取触发状态
     */
    TriggerStatus getStatus();
}
```

### 1.3 ActionNode 动作节点接口

```java
/**
 * 动作节点接口
 * 负责执行具体的业务操作
 */
public interface ActionNode extends DynamicNode {
    
    /**
     * 动作类型枚举
     */
    enum ActionType {
        HTTP_REQUEST,   // HTTP请求
        DATABASE_QUERY, // 数据库查询
        FILE_OPERATION, // 文件操作
        EMAIL_SEND,     // 邮件发送
        MESSAGE_SEND,   // 消息发送
        DATA_TRANSFORM, // 数据转换
        CUSTOM          // 自定义动作
    }
    
    /**
     * 执行动作
     */
    ActionResult executeAction(ActionContext context);
    
    /**
     * 获取动作类型
     */
    ActionType getActionType();
    
    /**
     * 是否支持批量操作
     */
    boolean supportsBatchOperation();
    
    /**
     * 批量执行动作
     */
    List<ActionResult> executeBatchAction(List<ActionContext> contexts);
}
```

## 2. 数据处理和编排机制

### 2.1 数据拆分器设计

```java
/**
 * 数据拆分器
 * 实现类似 Zapier 的数据自动拆分功能
 */
@Component
public class DataSplitter {
    
    /**
     * 拆分策略枚举
     */
    public enum SplitStrategy {
        AUTO_DETECT,    // 自动检测
        FORCE_ARRAY,    // 强制数组拆分
        SINGLE_ITEM,    // 单项处理
        CUSTOM_PATH     // 自定义路径拆分
    }
    
    /**
     * 拆分数据
     */
    public List<DataItem> splitData(Object data, SplitStrategy strategy, String customPath) {
        List<DataItem> result = new ArrayList<>();
        
        switch (strategy) {
            case AUTO_DETECT:
                return autoDetectAndSplit(data);
            case FORCE_ARRAY:
                return forceArraySplit(data);
            case SINGLE_ITEM:
                return Collections.singletonList(new DataItem(data, 0, 1));
            case CUSTOM_PATH:
                return customPathSplit(data, customPath);
            default:
                throw new IllegalArgumentException("Unsupported split strategy: " + strategy);
        }
    }
    
    /**
     * 自动检测数据类型并拆分
     */
    private List<DataItem> autoDetectAndSplit(Object data) {
        if (data instanceof List) {
            return splitList((List<?>) data);
        } else if (data instanceof String) {
            return splitJsonString((String) data);
        } else if (data instanceof Map) {
            return splitMap((Map<?, ?>) data);
        } else {
            return Collections.singletonList(new DataItem(data, 0, 1));
        }
    }
    
    /**
     * 拆分列表数据
     */
    private List<DataItem> splitList(List<?> list) {
        List<DataItem> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(new DataItem(list.get(i), i, list.size()));
        }
        return result;
    }
}
```

### 2.2 执行编排器设计

```java
/**
 * 执行编排器
 * 负责协调多个节点的执行顺序和数据流转
 */
@Component
public class ExecutionOrchestrator {
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private DataSplitter dataSplitter;
    
    @Autowired
    private ContextManager contextManager;
    
    /**
     * 编排策略枚举
     */
    public enum OrchestrationStrategy {
        SEQUENTIAL,     // 顺序执行
        PARALLEL,       // 并行执行
        DATA_DRIVEN,    // 数据驱动（推荐）
        CONDITIONAL     // 条件执行
    }
    
    /**
     * 执行工作流编排
     */
    public CompletableFuture<OrchestrationResult> orchestrate(
            WorkflowDefinition workflow, 
            TriggerData triggerData,
            OrchestrationStrategy strategy) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (strategy) {
                    case DATA_DRIVEN:
                        return executeDataDrivenOrchestration(workflow, triggerData);
                    case SEQUENTIAL:
                        return executeSequentialOrchestration(workflow, triggerData);
                    case PARALLEL:
                        return executeParallelOrchestration(workflow, triggerData);
                    case CONDITIONAL:
                        return executeConditionalOrchestration(workflow, triggerData);
                    default:
                        throw new IllegalArgumentException("Unsupported strategy: " + strategy);
                }
            } catch (Exception e) {
                return OrchestrationResult.failure(e);
            }
        });
    }
    
    /**
     * 数据驱动编排（推荐策略）
     * 类似 Zapier 的处理方式
     */
    private OrchestrationResult executeDataDrivenOrchestration(
            WorkflowDefinition workflow, 
            TriggerData triggerData) {
        
        // 1. 拆分触发数据
        List<DataItem> dataItems = dataSplitter.splitData(
            triggerData.getData(), 
            DataSplitter.SplitStrategy.AUTO_DETECT, 
            null
        );
        
        List<WorkflowExecution> executions = new ArrayList<>();
        
        // 2. 为每个数据项创建独立的工作流执行
        for (DataItem dataItem : dataItems) {
            try {
                // 创建执行上下文
                ExecutionContext context = contextManager.createContext(workflow, dataItem);
                
                // 启动工作流实例
                WorkflowInstance instance = workflowEngine.start(
                    new TriggerInstance()
                        .workflowId(workflow.getId())
                        .data(dataItem.getData())
                        .context(context)
                );
                
                executions.add(new WorkflowExecution(instance, dataItem, context));
                
            } catch (Exception e) {
                log.error("Failed to execute workflow for data item: " + dataItem, e);
                // 根据错误处理策略决定是否继续
                if (workflow.getErrorHandling().isStopOnError()) {
                    break;
                }
            }
        }
        
        return OrchestrationResult.success(executions);
    }
}
```

## 3. 与 Effektif 集成的适配器

### 3.1 DynamicActivityType 适配器

```java
/**
 * 动态活动类型适配器
 * 将动态节点适配到 Effektif 的 ActivityType 接口
 */
public class DynamicActivityType implements ActivityType<DynamicActivity> {
    
    private final DynamicNode dynamicNode;
    private final NodeConfig nodeConfig;
    
    public DynamicActivityType(DynamicNode dynamicNode, NodeConfig nodeConfig) {
        this.dynamicNode = dynamicNode;
        this.nodeConfig = nodeConfig;
    }
    
    @Override
    public void execute(ActivityInstanceImpl activityInstance) {
        try {
            // 1. 创建节点上下文
            NodeContext context = createNodeContext(activityInstance);
            
            // 2. 执行动态节点
            CompletableFuture<NodeResult> future = dynamicNode.execute(context);
            
            // 3. 处理执行结果
            if (dynamicNode.getNodeType() == DynamicNode.NodeType.TRIGGER) {
                // 触发节点异步处理
                handleTriggerNodeExecution(activityInstance, future);
            } else {
                // 动作节点同步处理
                handleActionNodeExecution(activityInstance, future);
            }
            
        } catch (Exception e) {
            log.error("Failed to execute dynamic node", e);
            activityInstance.setException(e);
            activityInstance.end();
        }
    }
    
    /**
     * 创建节点执行上下文
     */
    private NodeContext createNodeContext(ActivityInstanceImpl activityInstance) {
        return NodeContext.builder()
            .activityInstance(activityInstance)
            .workflowInstance(activityInstance.getWorkflowInstance())
            .variables(activityInstance.getVariables())
            .nodeConfig(nodeConfig)
            .build();
    }
    
    /**
     * 处理触发节点执行
     */
    private void handleTriggerNodeExecution(
            ActivityInstanceImpl activityInstance, 
            CompletableFuture<NodeResult> future) {
        
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                // 处理触发数据
                Object data = result.getData();
                if (data != null) {
                    // 设置输出变量
                    setOutputVariables(activityInstance, result);
                    // 继续执行下一个活动
                    activityInstance.onwards();
                } else {
                    // 没有数据，等待下次触发
                    activityInstance.setWorkState("waiting");
                }
            } else {
                // 处理错误
                activityInstance.setException(result.getError());
                activityInstance.end();
            }
        }).exceptionally(throwable -> {
            activityInstance.setException(throwable);
            activityInstance.end();
            return null;
        });
    }
}

## 4. 具体实现示例

### 4.1 HTTP 触发节点实现

```java
/**
 * HTTP 触发节点实现
 * 类似 Zapier 的 Webhook 触发器
 */
@Component
public class HttpTriggerNode implements TriggerNode {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DataSplitter dataSplitter;

    @Autowired
    private WorkflowEngine workflowEngine;

    private NodeConfig config;
    private ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;

    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER;
    }

    @Override
    public TriggerStrategy getTriggerStrategy() {
        return TriggerStrategy.POLLING;
    }

    @Override
    public void initialize(NodeConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    @Override
    public CompletableFuture<Void> startTrigger() {
        isRunning = true;

        return CompletableFuture.runAsync(() -> {
            int pollInterval = config.getInt("pollInterval", 60);
            scheduler.scheduleWithFixedDelay(
                this::pollData,
                0,
                pollInterval,
                TimeUnit.SECONDS
            );
        });
    }

    @Override
    public void stopTrigger() {
        isRunning = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public List<WorkflowExecution> processTriggerData(Object data, TriggerContext context) {
        List<WorkflowExecution> executions = new ArrayList<>();

        // 1. 拆分数据
        List<DataItem> dataItems = dataSplitter.splitData(
            data,
            DataSplitter.SplitStrategy.AUTO_DETECT,
            null
        );

        // 2. 为每个数据项创建工作流执行
        for (DataItem dataItem : dataItems) {
            try {
                // 创建触发实例
                TriggerInstance triggerInstance = new TriggerInstance()
                    .workflowId(context.getWorkflowId())
                    .data(dataItem.getData())
                    .sourceId("http-trigger")
                    .metadata(createMetadata(dataItem));

                // 启动工作流
                WorkflowInstance instance = workflowEngine.start(triggerInstance);
                executions.add(new WorkflowExecution(instance, dataItem));

            } catch (Exception e) {
                log.error("Failed to start workflow for data item: " + dataItem, e);
            }
        }

        return executions;
    }

    /**
     * 轮询数据
     */
    private void pollData() {
        if (!isRunning) return;

        try {
            String apiUrl = config.getString("apiUrl");
            Map<String, Object> params = buildRequestParams();

            // 发起 HTTP 请求
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                buildHttpEntity(params),
                String.class
            );

            // 解析响应
            Object data = parseResponse(response.getBody());

            // 检查是否有新数据
            if (hasNewData(data)) {
                // 创建触发上下文
                TriggerContext context = TriggerContext.builder()
                    .workflowId(config.getString("workflowId"))
                    .triggerId(config.getString("triggerId"))
                    .build();

                // 处理数据
                processTriggerData(data, context);

                // 更新状态
                updateLastPollTime();
            }

        } catch (Exception e) {
            log.error("Polling failed", e);
        }
    }

    @Override
    public CompletableFuture<NodeResult> execute(NodeContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 触发节点的执行逻辑
                Object inputData = context.getInputData();

                // 处理输入数据
                Object processedData = processInputData(inputData);

                return NodeResult.success(processedData);

            } catch (Exception e) {
                return NodeResult.failure(e);
            }
        });
    }
}
```
```
