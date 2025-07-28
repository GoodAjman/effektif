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
package com.effektif.workflow.api;

import com.effektif.workflow.api.model.*;
import com.effektif.workflow.api.query.WorkflowInstanceQuery;
import com.effektif.workflow.api.query.WorkflowQuery;
import com.effektif.workflow.api.workflow.ExecutableWorkflow;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;

import java.util.List;


/**
 * 工作流引擎主接口 - 提供工作流管理和执行的核心功能
 *
 * 这是Effektif工作流引擎的门面接口，采用门面模式(Facade Pattern)设计，
 * 为客户端提供统一的工作流操作入口点。
 *
 * 主要功能包括：
 * 1. 工作流定义的部署和管理
 * 2. 工作流实例的启动和控制
 * 3. 工作流执行过程中的消息传递
 * 4. 工作流变量的读写操作
 *
 * 使用示例：
 * <pre>{@code
 * Configuration config = new MemoryConfiguration();
 * config.start();
 * WorkflowEngine engine = config.getWorkflowEngine();
 *
 * // 部署工作流
 * ExecutableWorkflow workflow = new ExecutableWorkflow()...;
 * Deployment deployment = engine.deployWorkflow(workflow);
 *
 * // 启动工作流实例
 * WorkflowInstance instance = engine.start(new TriggerInstance()
 *   .workflowId(deployment.getWorkflowId()));
 * }</pre>
 *
 * @see Configuration 工作流引擎配置接口
 * @see ExecutableWorkflow 可执行工作流定义
 * @see WorkflowInstance 工作流实例
 * @author Tom Baeyens
 */
public interface WorkflowEngine {

  /**
   * 验证并部署工作流定义
   *
   * 该方法会执行以下步骤：
   * 1. 解析工作流定义，验证语法和语义
   * 2. 如果验证通过，将工作流存储到持久化层
   * 3. 将编译后的工作流缓存到内存中
   * 4. 如果工作流包含触发器，注册触发器
   *
   * @param workflow 要部署的可执行工作流定义
   * @return 部署结果，包含工作流ID和可能的错误信息
   * @throws RuntimeException 如果部署过程中发生系统错误
   */
  Deployment deployWorkflow(ExecutableWorkflow workflow);

  /**
   * 根据查询条件查找工作流定义
   *
   * @param workflowQuery 查询条件，可以按工作流ID、源ID等条件查询
   * @return 符合条件的工作流定义列表
   */
  List<ExecutableWorkflow> findWorkflows(WorkflowQuery workflowQuery);

  /**
   * 根据查询条件删除工作流定义
   *
   * 注意：删除工作流定义不会影响已经运行的工作流实例
   *
   * @param workflowQuery 删除条件
   */
  void deleteWorkflows(WorkflowQuery workflowQuery);

  /**
   * 启动新的工作流实例
   *
   * 该方法会：
   * 1. 根据触发器实例中的工作流ID查找工作流定义
   * 2. 创建新的工作流实例
   * 3. 初始化工作流变量
   * 4. 执行起始活动
   *
   * @param triggerInstance 触发器实例，包含工作流ID和初始数据
   * @return 创建的工作流实例
   * @throws RuntimeException 如果工作流不存在或启动失败
   */
  WorkflowInstance start(TriggerInstance triggerInstance);

  /**
   * 向活动实例发送消息以推进工作流执行
   *
   * 这是工作流执行的核心方法，通常用于：
   * 1. 完成等待中的任务(ReceiveTask)
   * 2. 向活动传递外部数据
   * 3. 触发活动的状态转换
   *
   * 执行过程：
   * 1. 锁定工作流实例以确保并发安全
   * 2. 查找目标活动实例
   * 3. 将消息传递给活动实例处理
   * 4. 根据活动处理结果推进工作流
   * 5. 更新并解锁工作流实例
   *
   * @param message 消息对象，包含目标活动实例ID和消息数据
   * @return 更新后的工作流实例
   * @throws RuntimeException 如果活动实例不存在或消息处理失败
   */
  WorkflowInstance send(Message message);

  /**
   * 手动移动工作流实例到指定活动
   *
   * 这是一个管理功能，允许管理员手动干预工作流执行，
   * 将工作流从当前状态强制移动到指定的活动。
   *
   * @param workflowInstanceId 工作流实例ID
   * @param activityInstanceId 当前活动实例ID（可选）
   * @param newActivityId 目标活动ID
   * @return 更新后的工作流实例
   */
  WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String activityInstanceId, String newActivityId);

  /**
   * 手动移动工作流实例到指定活动（简化版本）
   *
   * @param workflowInstanceId 工作流实例ID
   * @param newActivityId 目标活动ID
   * @return 更新后的工作流实例
   */
  WorkflowInstance move(WorkflowInstanceId workflowInstanceId, String newActivityId);

  /**
   * 取消工作流实例
   *
   * 取消操作会：
   * 1. 停止所有正在执行的活动
   * 2. 清理相关的定时任务
   * 3. 将工作流实例标记为已取消状态
   *
   * @param workflowInstanceId 要取消的工作流实例ID
   * @return 取消后的工作流实例
   */
  WorkflowInstance cancel(WorkflowInstanceId workflowInstanceId);

  /**
   * 获取工作流实例的变量值
   *
   * @param workflowInstanceId 工作流实例ID
   * @return 工作流实例级别的所有变量值
   */
  VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId);

  /**
   * 获取指定活动实例的变量值
   *
   * @param workflowInstanceId 工作流实例ID
   * @param activityInstanceId 活动实例ID
   * @return 活动实例级别的变量值
   */
  VariableValues getVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId);

  /**
   * 设置工作流实例的变量值
   *
   * @param workflowInstanceId 工作流实例ID
   * @param variableValues 要设置的变量值
   */
  void setVariableValues(WorkflowInstanceId workflowInstanceId, VariableValues variableValues);

  /**
   * 设置指定活动实例的变量值
   *
   * @param workflowInstanceId 工作流实例ID
   * @param activityInstanceId 活动实例ID
   * @param variableValues 要设置的变量值
   */
  void setVariableValues(WorkflowInstanceId workflowInstanceId, String activityInstanceId, VariableValues variableValues);

  /**
   * 根据查询条件查找工作流实例
   *
   * @param query 查询条件，可以按工作流ID、状态、创建时间等条件查询
   * @return 符合条件的工作流实例列表
   */
  List<WorkflowInstance> findWorkflowInstances(WorkflowInstanceQuery query);

  /**
   * 根据查询条件删除工作流实例
   *
   * 注意：删除操作会清理所有相关数据，包括活动实例、变量、定时任务等
   *
   * @param query 删除条件
   */
  void deleteWorkflowInstances(WorkflowInstanceQuery query);
}
