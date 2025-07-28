# Effektif 工作流执行机制深度分析

本文档详细分析 Effektif 工作流引擎的执行机制，解释活动定义与活动实例的关系，以及工作流推进的内部逻辑。

## 1. 活动定义与活动实例的关系

### 1.1 核心概念

```java
// 活动定义 (Activity) - 静态模板
.activity("greet", new ReceiveTask()  // "greet" 是活动定义ID
    .name("问候任务")
    .transitionTo("end"))

// 活动实例 (ActivityInstance) - 运行时实例
// 当工作流执行到 "greet" 活动时，会创建一个 ActivityInstance
```

### 1.2 关系映射机制

```java
// 在 ActivityInstanceImpl 类中
public class ActivityInstanceImpl extends ScopeInstanceImpl {
    public String id;              // 活动实例的唯一ID (运行时生成)
    public ActivityImpl activity;  // 指向活动定义的引用
    
    // 活动实例ID与活动定义ID的关系
    public String getActivityId() {
        return activity.id;  // 返回活动定义的ID，如 "greet"
    }
}
```

### 1.3 查找机制源码分析

```java
// 在 ScopeInstanceImpl 类中
public ActivityInstanceImpl findActivityInstanceByActivityId(String activityDefinitionId) {
    if (activityDefinitionId == null) {
        return null;
    }
    if (activityInstances != null) {
        for (ActivityInstanceImpl activityInstance : activityInstances) {
            // 递归查找，支持嵌套活动
            ActivityInstanceImpl theOne = activityInstance.findActivityInstanceByActivityId(activityDefinitionId);
            if (theOne != null) {
                return theOne;
            }
        }
    }
    return null;
}

// 在 ActivityInstanceImpl 类中
@Override
public ActivityInstanceImpl findActivityInstance(String activityInstanceId) {
    if (activityInstanceId == null) {
        return null;
    }
    // 首先检查是否是当前活动实例
    if (activityInstanceId.equals(this.id)) {
        return this;
    }
    // 然后在子活动中查找
    return super.findActivityInstance(activityInstanceId);
}
```

## 2. 工作流执行推进机制

### 2.1 活动实例创建过程

```java
// 在 ScopeInstanceImpl 类中
public ActivityInstanceImpl createActivityInstance(ActivityImpl activity) {
    // 1. 生成唯一的活动实例ID
    String activityInstanceId = workflowInstance.generateNextActivityInstanceId();
    
    // 2. 创建活动实例对象
    ActivityInstanceImpl activityInstance = new ActivityInstanceImpl(this, activity, activityInstanceId);
    
    // 3. 设置初始状态
    if (activity.isMultiInstance()) {
        activityInstance.setWorkState(STATE_STARTING_MULTI_CONTAINER);
    } else {
        activityInstance.setWorkState(STATE_STARTING);
    }
    
    // 4. 添加到工作队列
    workflowInstance.addWork(activityInstance);
    
    // 5. 设置开始时间
    activityInstance.start = Time.now();
    
    return activityInstance;
}
```

### 2.2 工作流推进的核心循环

```java
// 在 WorkflowEngineImpl 类中
public WorkflowInstance startExecute(WorkflowInstanceImpl workflowInstance) {
    WorkflowImpl workflow = workflowInstance.workflow;
    
    // 执行起始活动
    if (workflow.startActivities != null) {
        for (ActivityImpl startActivityDefinition : workflow.startActivities) {
            if (workflowInstance.startActivityIds == null
                    || workflowInstance.startActivityIds.contains(startActivityDefinition.getId())) {
                // 创建并执行起始活动实例
                workflowInstance.execute(startActivityDefinition);
            }
        }
    }
    
    // 执行工作队列中的活动
    executeWork(workflowInstance);
    
    return workflowInstance.toWorkflowInstance();
}

// 工作执行循环
protected void executeWork(WorkflowInstanceImpl workflowInstance) {
    while (workflowInstance.hasWork()) {
        ActivityInstanceImpl activityInstance = workflowInstance.getNextWork();
        if (activityInstance != null) {
            executeActivityInstance(activityInstance);
        }
    }
}
```

### 2.3 活动执行机制

```java
// 在 ActivityInstanceImpl 类中
public void execute() {
    // 通知监听器活动开始
    if (workflow.workflowEngine.notifyActivityInstanceStarted(this)) {
        // 委托给具体的活动类型执行
        activity.activityType.execute(this);
    }
}

// 活动完成后的流转逻辑
@Override
public void onwards() {
    // 1. 结束当前活动实例
    end();
    
    boolean isTransitionTaken = false;
    
    // 2. 处理所有输出流转
    if (activity.hasOutgoingTransitions()) {
        for (TransitionImpl transition : activity.outgoingTransitions) {
            // 3. 评估流转条件
            ConditionImpl condition = transition.condition;
            if (condition != null ? condition.eval(this) : true) {
                isTransitionTaken = true;
                // 4. 执行流转
                takeTransition(transition);
            }
        }
    }
    
    // 5. 如果没有流转，向父级传播
    if (!isTransitionTaken) {
        propagateToParent();
    }
}
```

### 2.4 流转执行机制

```java
// 在 ActivityInstanceImpl 类中
public void takeTransition(TransitionImpl transition) {
    // 1. 记录流转历史
    if (transition.id != null || activity.activityType.saveTransitionsTaken()) {
        addTransitionTaken(transition.id);
    }
    
    ActivityInstanceImpl toActivityInstance = null;
    ActivityImpl to = transition.to;
    
    if (to != null) {
        // 2. 结束当前活动
        end();
        
        // 3. 在父作用域中创建目标活动实例
        toActivityInstance = parent.createActivityInstance(to);
        
        // 4. 目标活动实例会自动加入工作队列，等待执行
    } else {
        // 悬空流转，向父级传播
        end();
        propagateToParent();
    }
    
    // 5. 通知监听器流转已执行
    workflow.workflowEngine.notifyTransitionTaken(this, transition, toActivityInstance);
}
```

## 3. 消息处理机制

### 3.1 消息发送流程

```java
// 在 WorkflowEngineImpl 类中
@Override
public WorkflowInstance send(Message message) {
    // 1. 锁定工作流实例
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(message.getWorkflowInstanceId());
    return send(message, workflowInstance);
}

public WorkflowInstance send(Message message, WorkflowInstanceImpl workflowInstance) {
    // 2. 设置临时数据
    Map<String, Object> transientData = message.getTransientData();
    if (transientData != null) {
        for (String key : transientData.keySet()) {
            workflowInstance.setTransientProperty(key, transientData.get(key));
        }
    }
    
    // 3. 查找目标活动实例
    String activityInstanceId = message.getActivityInstanceId();
    ActivityInstanceImpl activityInstance = workflowInstance.findActivityInstance(activityInstanceId);
    
    if (activityInstance == null) {
        workflowInstanceStore.unlockWorkflowInstance(workflowInstance);
        throw new RuntimeException("Activity instance " + activityInstanceId + " not in workflow instance");
    }
    
    // 4. 将消息传递给活动实例
    activityInstance.message(message);
    
    // 5. 执行后续工作
    executeWork(workflowInstance);
    
    // 6. 保存并解锁
    workflowInstanceStore.flushAndUnlock(workflowInstance);
    
    return workflowInstance.toWorkflowInstance();
}
```

### 3.2 ReceiveTask 的消息处理

```java
// 在 ReceiveTaskImpl 类中 (活动类型实现)
@Override
public void execute(ActivityInstanceImpl activityInstance) {
    // ReceiveTask 在执行时进入等待状态
    // 不调用 activityInstance.onwards()，保持活动实例开放
    // 等待外部消息通过 message() 方法推进
}

@Override
public void message(ActivityInstanceImpl activityInstance, Message message) {
    // 1. 处理消息数据
    if (message.getData() != null) {
        for (Map.Entry<String, Object> entry : message.getData().entrySet()) {
            activityInstance.setVariableValue(entry.getKey(), entry.getValue());
        }
    }
    
    // 2. 完成活动并继续流转
    activityInstance.onwards();
}
```

## 4. 状态管理机制

### 4.1 活动实例状态

```java
public class ActivityInstanceImpl extends ScopeInstanceImpl {
    // 活动实例状态常量
    public static final String STATE_STARTING = "starting";
    public static final String STATE_STARTING_MULTI_CONTAINER = "startingMultiParent";
    public static final String STATE_STARTING_MULTI_INSTANCE = "startingMultiInstance";
    public static final String STATE_PROPAGATE_TO_PARENT = "propagateToParent";
    public static final String STATE_JOINING = "joining";
    
    public String workState;  // 当前工作状态
}
```

### 4.2 工作队列管理

```java
// 在 WorkflowInstanceImpl 类中
public class WorkflowInstanceImpl extends ScopeInstanceImpl {
    public Queue<ActivityInstanceImpl> work;      // 同步工作队列
    public Queue<ActivityInstanceImpl> workAsync; // 异步工作队列
    
    public void addWork(ActivityInstanceImpl activityInstance) {
        if (isWorkAsync(activityInstance)) {
            addAsyncWork(activityInstance);
        } else {
            addSyncWork(activityInstance);
        }
    }
    
    public ActivityInstanceImpl getNextWork() {
        ActivityInstanceImpl nextWork = work != null ? work.poll() : null;
        if (nextWork != null && updates != null) {
            getUpdates().isWorkChanged = true;
        }
        return nextWork;
    }
}
```

## 5. 实际执行示例分析

### 5.1 工作流定义
```java
ExecutableWorkflow workflow = new ExecutableWorkflow()
    .activity("start", new StartEvent().transitionTo("greet"))
    .activity("greet", new ReceiveTask().transitionTo("end"))
    .activity("end", new EndEvent());
```

### 5.2 执行过程分析

1. **启动阶段**:
   ```java
   // 创建工作流实例
   WorkflowInstanceImpl instance = new WorkflowInstanceImpl(...);
   
   // 执行起始活动
   ActivityInstanceImpl startInstance = instance.createActivityInstance(startActivity);
   // startInstance.id = "1", startInstance.activity.id = "start"
   ```

2. **StartEvent 执行**:
   ```java
   // StartEvent 立即完成并流转
   startInstance.execute();  // StartEvent 的 execute() 直接调用 onwards()
   startInstance.onwards();  // 触发到 "greet" 的流转
   ```

3. **流转到 ReceiveTask**:
   ```java
   // 创建 greet 活动实例
   ActivityInstanceImpl greetInstance = instance.createActivityInstance(greetActivity);
   // greetInstance.id = "2", greetInstance.activity.id = "greet"
   
   // ReceiveTask 进入等待状态
   greetInstance.execute();  // 不调用 onwards()，保持开放状态
   ```

4. **查找和消息发送**:
   ```java
   // 通过活动定义ID查找活动实例
   ActivityInstance found = instance.findOpenActivityInstance("greet");
   // 返回 greetInstance，其中 found.getId() = "2", found.getActivityId() = "greet"
   
   // 发送消息
   Message message = new Message()
       .workflowInstanceId(instance.getId())
       .activityInstanceId(found.getId())  // 使用活动实例ID "2"
       .data("response", "完成");
   ```

5. **消息处理和完成**:
   ```java
   // 消息处理
   greetInstance.message(message);  // 设置变量并调用 onwards()
   greetInstance.onwards();         // 流转到 "end" 活动
   
   // 创建并执行 EndEvent
   ActivityInstanceImpl endInstance = instance.createActivityInstance(endActivity);
   endInstance.execute();           // EndEvent 结束工作流
   ```

这个分析展示了活动定义ID（如"greet"）与活动实例ID（如"2"）之间的映射关系，以及工作流引擎如何通过工作队列和状态管理来推进执行流程。
