/*
 * Copyright 2014 Effektif GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.effektif.workflow.impl;

import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.model.*;
import com.effektif.workflow.api.query.WorkflowInstanceQuery;
import com.effektif.workflow.api.query.WorkflowQuery;
import com.effektif.workflow.api.workflow.ExecutableWorkflow;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.effektif.workflow.impl.configuration.Brewable;
import com.effektif.workflow.impl.configuration.Brewery;
import com.effektif.workflow.impl.data.DataTypeService;
import com.effektif.workflow.impl.job.Job;
import com.effektif.workflow.impl.util.Exceptions;
import com.effektif.workflow.impl.util.Time;
import com.effektif.workflow.impl.workflow.ActivityImpl;
import com.effektif.workflow.impl.workflow.TransitionImpl;
import com.effektif.workflow.impl.workflow.WorkflowImpl;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;
import com.effektif.workflow.impl.workflowinstance.LockImpl;
import com.effektif.workflow.impl.workflowinstance.ScopeInstanceImpl;
import com.effektif.workflow.impl.workflowinstance.WorkflowInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 工作流引擎核心实现类
 *
 * 这是WorkflowEngine接口的主要实现，负责协调各个组件完成工作流的
 * 部署、启动、执行和管理等核心功能。
 *
 * 架构设计：
 * 1. 实现Brewable接口，支持依赖注入
 * 2. 采用组合模式，聚合各种服务组件
 * 3. 使用策略模式，支持不同的存储和执行策略
 * 4. 实现观察者模式，支持工作流执行监听器
 *
 * 核心组件：
 * - WorkflowStore: 工作流定义存储
 * - WorkflowInstanceStore: 工作流实例存储
 * - WorkflowCache: 工作流定义缓存
 * - ExecutorService: 异步执行服务
 * - DataTypeService: 数据类型服务
 *
 * 并发控制：
 * - 使用工作流实例锁机制确保并发安全
 * - 支持重试机制处理锁竞争
 * - 异步执行避免阻塞主线程
 *
 * @author Tom Baeyens
 */
public class WorkflowEngineImpl implements WorkflowEngine, Brewable {

  public static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

  /** 工作流引擎唯一标识 */
  public String id;

  /** 异步执行服务，用于处理后台任务和定时任务 */
  public ExecutorService executorService;

  /** 工作流定义缓存，提高查找性能 */
  public WorkflowCache workflowCache;

  /** 工作流定义存储接口 */
  public WorkflowStore workflowStore;

  /** 工作流实例存储接口 */
  public WorkflowInstanceStore workflowInstanceStore;

  /** 工作流引擎配置 */
  public Configuration configuration;

  /** 工作流执行监听器列表，用于监控和审计 */
  public List<WorkflowExecutionListener> workflowExecutionListeners;

  /** 数据类型服务，处理变量类型转换和验证 */
  public DataTypeService dataTypeService;


  /**
   * 依赖注入回调方法
   *
   * 当Brewery容器初始化此组件时调用，用于注入所需的依赖组件。
   * 这是依赖注入模式的实现，避免了硬编码的依赖关系。
   */
  @Override
  public void brew(Brewery brewery) {
    this.id = brewery.get(WorkflowEngineConfiguration.class).getWorkflowEngineId();
    this.configuration = brewery.get(Configuration.class);
    this.executorService = brewery.get(ExecutorService.class);
    this.workflowCache = brewery.get(WorkflowCache.class);
    this.workflowStore = brewery.get(WorkflowStore.class);
    this.workflowInstanceStore = brewery.get(WorkflowInstanceStore.class);
    this.dataTypeService = brewery.get(DataTypeService.class);
  }

  /**
   * 引擎启动方法
   *
   * 预留的启动钩子，可以在子类中重写以执行启动时的初始化逻辑。
   */
  public void startup() {
  }

  /**
   * 引擎关闭方法
   *
   * 优雅关闭工作流引擎，清理资源并停止后台服务。
   */
  public void shutdown() {
    executorService.shutdown();
  }

  /// 工作流管理方法 ////////////////////////////////////////////////////////////

  /**
   * 部署工作流定义
   *
   * 部署过程包括以下步骤：
   * 1. 使用WorkflowParser解析和验证工作流定义
   * 2. 如果验证通过，生成工作流ID（如果未指定）
   * 3. 设置创建时间并存储到持久化层
   * 4. 如果包含触发器，注册触发器
   * 5. 将编译后的工作流放入缓存
   *
   * @param workflow 要部署的工作流定义
   * @return 部署结果，包含工作流ID和验证问题列表
   */
  @Override
  public Deployment deployWorkflow(ExecutableWorkflow workflow) {
    if (log.isDebugEnabled()) {
      log.debug("Deploying workflow");
    }

    // 第一步：解析和验证工作流定义
    WorkflowParser parser = new WorkflowParser(configuration);
    parser.parse(workflow);

    // 第二步：如果验证通过，进行部署
    if (!parser.hasErrors()) {
      WorkflowImpl workflowImpl = parser.getWorkflow();
      WorkflowId workflowId;

      // 生成工作流ID（如果未指定）
      if (workflow.getId()==null) {
        workflowId = workflowStore.generateWorkflowId();
        workflow.setId(workflowId);
      }

      // 设置元数据并存储
      workflow.setCreateTime(Time.now());
      workflowImpl.id = workflow.getId();
      workflowStore.insertWorkflow(workflow);

      // 注册触发器（如果存在）
      if (workflowImpl.trigger!=null) {
        workflowImpl.trigger.published(workflowImpl);
      }

      // 缓存编译后的工作流
      workflowCache.put(workflowImpl);
    }

    return new Deployment(workflow.getId(), parser.getIssues());
  }

  @Override
  public List<ExecutableWorkflow> findWorkflows(WorkflowQuery workflowQuery) {
    return workflowStore.findWorkflows(workflowQuery);
  }

  @Override
  public void deleteWorkflows(WorkflowQuery workflowQuery) {
    workflowStore.deleteWorkflows(workflowQuery);
  }

  public WorkflowInstance start(TriggerInstance triggerInstance) {
    WorkflowInstanceImpl workflowInstance = startInitialize(triggerInstance);
    return startExecute(workflowInstance);
  }

  /** first part of starting a new workflow instance: creating the workflow instance and applying the trigger data */
  public WorkflowInstanceImpl startInitialize(TriggerInstance triggerInstance) {
    WorkflowId workflowId = getLatestWorkflowId(triggerInstance);
    WorkflowImpl workflow = getWorkflowImpl(workflowId);

    LockImpl lock = new LockImpl();
    lock.setTime(Time.now());
    lock.setOwner(getId());

    WorkflowInstanceId workflowInstanceId = triggerInstance.getWorkflowInstanceId();
    if (workflowInstanceId==null) {
      workflowInstanceId = workflowInstanceStore.generateWorkflowInstanceId();
    }

    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl(
            configuration,
            workflow,
            workflowInstanceId,
            triggerInstance,
            lock,
            triggerInstance.getTransientData());

    if (log.isDebugEnabled()) log.debug("Created "+workflowInstance);

    if (workflow.trigger!=null) {
      workflow.trigger.applyTriggerData(workflowInstance, triggerInstance);
    } else {
      workflowInstance.setVariableValues(triggerInstance);
    }
    
    notifyWorkflowInstanceStarted(workflowInstance);

    return workflowInstance;
  }

  /** Second part of starting a new workflow instance: executing the start activities. */
  public WorkflowInstance startExecute(WorkflowInstanceImpl workflowInstance) {
    WorkflowImpl workflow = workflowInstance.workflow;
    if (log.isDebugEnabled()) log.debug("Starting "+workflowInstance);

    if (workflow.startActivities!=null) {
      for (ActivityImpl startActivityDefinition: workflow.startActivities) {
        if (workflowInstance.startActivityIds == null
                || workflowInstance.startActivityIds.contains(startActivityDefinition.getId())) {
          workflowInstance.execute(startActivityDefinition);
        }
      }
    } else {
      workflowInstance.endAndPropagateToParent();
    }

    notifyInsert(workflowInstance);
    workflowInstanceStore.insertWorkflowInstance(workflowInstance);
    return workflowInstance.executeWork();
  }

  public WorkflowId getLatestWorkflowId(TriggerInstance triggerInstance) {
    WorkflowId workflowId = triggerInstance.getWorkflowId();
    if (workflowId==null) {
      if (triggerInstance.getSourceWorkflowId()!=null) {
        workflowId = workflowStore.findLatestWorkflowIdBySource(triggerInstance.getSourceWorkflowId());
        if (workflowId==null) throw new RuntimeException("No workflow found for source '"+triggerInstance.getSourceWorkflowId()+"'");
      } else {
        throw new RuntimeException("No workflow specified");
      }
    }
    return workflowId;
  }

  @Override
  public WorkflowInstance send(Message message) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(message.getWorkflowInstanceId());
    return send(message, workflowInstance);
  }

  /***
   * To manually move a workflowInstance from the current activityInstance to the specified activityInstance.
   * Any "work" in between will not be executed! Will probably be used during testing of your workflows...
   * Note: If your process contains a parallel gateway, and you move one of the two "instances" to an activity after the
   * merge-parallel gateway, things would get messy.... Because of that, the move does not allow this, it checks for #open activityInstances <= 1
   * Sub-processes are not taken into account ie, propagateToParent is not called.
   * @return WorkflowInstance is the to-activity was found and the move was executed, null otherwise.
   */
  public WorkflowInstance moveImpl(WorkflowInstanceImpl workflowInstanceImpl, String activityInstanceId, String newActivityId) {

    if(workflowInstanceImpl.lock == null) throw new RuntimeException("WorkflowInstance not locked!");

    if (log.isDebugEnabled()) log.debug("Moving workflowInstance to activityId: " + newActivityId);

    try {
      if (workflowInstanceImpl.activityInstances == null) {
        log.debug("ActivityInstances == null, returning without doing something.");
        return null;
      }

      ActivityInstanceImpl activityInstanceImpl = null;
      int openActCount = 0;
      if (activityInstanceId == null) {
        for (ActivityInstanceImpl activityInstance : workflowInstanceImpl.activityInstances) {
          if (!activityInstance.isEnded()) {
            activityInstanceImpl = activityInstance;
            openActCount++;
          }
        }
      } else {
        activityInstanceImpl = workflowInstanceImpl.findActivityInstanceByActivityId(activityInstanceId);
      }

      if (openActCount > 1)
        throw new RuntimeException("Move cannot be called on a workflowInstance with more than one open activityInstance. " +
                "Probably this workflowInstance is part of a paralell process...");

      if (workflowInstanceImpl.jobs != null) {
        Iterator<Job> jobIterator = workflowInstanceImpl.jobs.iterator();

        while (jobIterator.hasNext()) {
          Job job = jobIterator.next();
          if (job.getActivityInstanceId() != null && activityInstanceImpl != null
              && job.getActivityInstanceId().equals(activityInstanceImpl.getId())) {
            jobIterator.remove();
          }
        }
      }

      ActivityImpl activityImpl = workflowInstanceImpl.workflow.findActivityByIdLocal(newActivityId);
      if (activityImpl == null) throw new RuntimeException("To-activityId not found!");

      if (activityInstanceImpl != null && !activityInstanceImpl.isEnded()) activityInstanceImpl.end();
      if (workflowInstanceImpl.isEnded()) {
        workflowInstanceImpl.setEnd(null);
        workflowInstanceImpl.duration = 0L;
      }

      workflowInstanceImpl.execute(activityImpl);
      return workflowInstanceImpl.executeWork();
    } finally {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstanceImpl);
    }
  }

  @Override
  public WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String activityInstanceId, String newActivityId) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);

    return moveImpl(workflowInstance, activityInstanceId, newActivityId);
  }

  @Override
  public WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String newActivityId) {
    return move(workflowInstanceId, null, newActivityId);
  }

  public WorkflowInstance send(Message message, WorkflowInstanceImpl workflowInstance) {
    Map<String, Object> transientData = message.getTransientData();
    if (transientData !=null) {
      for (String key: transientData.keySet()) {
        workflowInstance.setTransientProperty(key, transientData.get(key));
      }
    }
    String activityInstanceId = message.getActivityInstanceId();
    ActivityInstanceImpl activityInstance = workflowInstance.findActivityInstance(activityInstanceId);
    if (activityInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstance);
      throw new RuntimeException("Activity instance "+activityInstanceId+" not in workflow instance");
    }
    if (log.isDebugEnabled())
      log.debug("Signalling "+activityInstance);
    ActivityImpl activity = activityInstance.getActivity();
    activity.activityType.message(activityInstance, message);
    return workflowInstance.executeWork();
  }

  @Override
  public WorkflowInstance cancel(WorkflowInstanceId workflowInstanceId) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);
    workflowInstance.cancel();
    return workflowInstance.toWorkflowInstance();
  }


  @Override
  public void deleteWorkflowInstances(WorkflowInstanceQuery query) {
    workflowInstanceStore.deleteWorkflowInstances(query);
  }

  @Override
  public List<WorkflowInstance> findWorkflowInstances(WorkflowInstanceQuery query) {
    List<WorkflowInstanceImpl> workflowInstances = workflowInstanceStore.findWorkflowInstances(query);
    return WorkflowInstanceImpl.toWorkflowInstances(workflowInstances);
  }

  /** retrieves the executable form of the workflow using the workflow cache */
  public WorkflowImpl getWorkflowImpl(WorkflowId workflowId) {
    WorkflowImpl workflowImpl = workflowCache.get(workflowId);
    if (workflowImpl==null) {
      ExecutableWorkflow workflow = workflowStore.loadWorkflowById(workflowId);
      if (workflow != null) {
        WorkflowParser parser = new WorkflowParser(configuration);
        workflowImpl = parser.parse(workflow);
        workflowCache.put(workflowImpl);
      }
    }
    return workflowImpl;
  }

  public WorkflowInstanceImpl lockWorkflowInstanceWithRetry(
          final WorkflowInstanceId workflowInstanceId) {
    Retry<WorkflowInstanceImpl> retry = new Retry<WorkflowInstanceImpl>() {
      @Override
      public WorkflowInstanceImpl tryOnce() {
        return workflowInstanceStore.lockWorkflowInstance(workflowInstanceId);
      }
      @Override
      protected void failedWaitingForRetry() {
        if (log.isDebugEnabled()) {
          log.debug("Locking workflow instance "+workflowInstanceId+" failed... retrying in "+wait+" millis");
        }
      }
      @Override
      protected void interrupted() {
        if (log.isDebugEnabled()) {
          log.debug("Waiting for workflow instance lock was interrupted");
        }
      }
      @Override
      protected void failedPermanent() {
        throw new RuntimeException("Couldn't lock workflow instance " + workflowInstanceId);
      }
    };
    return retry.tryManyTimes();
  }

  public String getId() {
    return id;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  public WorkflowCache getProcessDefinitionCache() {
    return workflowCache;
  }

  public WorkflowStore getWorkflowStore() {
    return workflowStore;
  }

  public WorkflowInstanceStore getWorkflowInstanceStore() {
    return workflowInstanceStore;
  }

  public void addWorkflowExecutionListener(WorkflowExecutionListener workflowExecutionListener) {
    if (workflowExecutionListeners==null) {
      workflowExecutionListeners = new ArrayList<>();
    }
    workflowExecutionListeners.add(workflowExecutionListener);
  }

  public void removeWorkflowExecutionListener(WorkflowExecutionListener workflowExecutionListener) {
    if (workflowExecutionListeners!=null) {
      workflowExecutionListeners.remove(workflowExecutionListener);
      if (workflowExecutionListeners.isEmpty()) {
        workflowExecutionListeners = null;
      }
    }
  }

  public List<WorkflowExecutionListener> getWorkflowExecutionListeners() {
    return workflowExecutionListeners;
  }

  public void setWorkflowExecutionListeners(List<WorkflowExecutionListener> workflowExecutionListeners) {
    this.workflowExecutionListeners = workflowExecutionListeners;
  }

  public void notifyWorkflowInstanceStarted(WorkflowInstanceImpl workflowInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.starting(workflowInstance);
      }
    }
  }

  public void notifyWorkflowInstanceEnded(WorkflowInstanceImpl workflowInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.ended(workflowInstance);
      }
    }
  }

  public boolean notifyActivityInstanceStarted(ActivityInstanceImpl activityInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        if (!workflowExecutionListener.starting(activityInstance)) {
          return false;
        }
      }
    }
    return true;
  }

  public void notifyUnlocked(WorkflowInstanceImpl workflowInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.unlocked(workflowInstance);
      }
    }
  }

  public void notifyFlush(WorkflowInstanceImpl workflowInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.flush(workflowInstance);
      }
    }
  }

  public void notifyActivityInstanceEnded(ActivityInstanceImpl activityInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.ended(activityInstance);
      }
    }
  }

  public void notifyTransitionTaken(ActivityInstanceImpl activityInstanceFrom, TransitionImpl transition, ActivityInstanceImpl activityInstanceTo) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.transitioning(activityInstanceFrom, transition, activityInstanceTo);
      }
    }
  }
  
  public void notifyInsert(WorkflowInstanceImpl workflowInstance) {
    if (workflowExecutionListeners!=null) {
      for (WorkflowExecutionListener workflowExecutionListener: workflowExecutionListeners) {
        workflowExecutionListener.insert(workflowInstance);
      }
    }
  }



  public VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId) {
    return getVariableValues(workflowInstanceId, null);
  }

  public VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId) {
    WorkflowInstanceImpl workflowInstance = workflowInstanceStore.getWorkflowInstanceImplById(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    VariableValues variableValues = new VariableValues();
    scopeInstance.collectVariableValues(variableValues);
    return variableValues;
  }

  public void setVariableValues(WorkflowInstanceId workflowInstanceId, VariableValues variableValues) {
    setVariableValues(workflowInstanceId, null, variableValues);
  }

  public void setVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId, VariableValues variableValues) {
    if (workflowInstanceId==null || variableValues==null) {
      return;
    }
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    if (scopeInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstance);
      throw new RuntimeException("Workflow instance "+workflowInstanceId+" didn't contain active activityInstanceId "+activityInstanceId);
    }
    Map<String, TypedValue> values = variableValues!=null ? variableValues.getValues() : null;
    if (values!=null) {
      for (String variableId : values.keySet()) {
        TypedValue typedValue = values.get(variableId);
        Object value = typedValue.getValue();
        scopeInstance.setVariableValue(variableId, value);
      }
    }
    workflowInstanceStore.flushAndUnlock(workflowInstance);
  }

  public void setVariableValue(WorkflowInstanceId workflowInstanceId, String activityInstanceId, String variableId, Object value) {
    WorkflowInstanceImpl workflowInstance = lockWorkflowInstanceWithRetry(workflowInstanceId);
    ScopeInstanceImpl scopeInstance = getScopeInstance(workflowInstance, activityInstanceId);
    if (scopeInstance==null) {
      workflowInstanceStore.unlockWorkflowInstance(workflowInstance);
      throw new RuntimeException("Workflow instance "+workflowInstanceId+" didn't contain active activityInstanceId "+activityInstanceId);
    }
    scopeInstance.setVariableValue(variableId, value);
    workflowInstanceStore.flushAndUnlock(workflowInstance);
  }

  protected ScopeInstanceImpl getScopeInstance(WorkflowInstanceImpl workflowInstance, String activityInstanceId) {
    ScopeInstanceImpl scopeInstance = workflowInstance;
    if (activityInstanceId!=null) {
      scopeInstance = workflowInstance.findActivityInstance(activityInstanceId);
      Exceptions.checkNotNull(scopeInstance);
    }
    return scopeInstance;
  }

  public void executeAsync(Runnable asyncWork) {
    executorService.execute(asyncWork);
  }
}
