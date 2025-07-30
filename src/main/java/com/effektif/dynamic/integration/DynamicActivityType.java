/*
 * Copyright 2024 Effektif GmbH.
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
package com.effektif.dynamic.integration;

import com.effektif.dynamic.node.DynamicNode;
import com.effektif.dynamic.node.NodeConfig;
import com.effektif.dynamic.node.NodeContext;
import com.effektif.dynamic.node.NodeResult;
import com.effektif.workflow.api.activities.DynamicActivity;
import com.effektif.workflow.impl.activity.AbstractActivityType;
import com.effektif.workflow.impl.activity.ActivityDescriptor;
import com.effektif.workflow.impl.workflowinstance.ActivityInstanceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 动态活动类型适配器
 * 
 * 将动态节点适配到 Effektif 的 ActivityType 接口，实现动态节点与
 * Effektif 工作流引擎的无缝集成。这个适配器负责：
 * 
 * 1. 接口适配：将 DynamicNode 接口适配到 Effektif 的 ActivityType 接口
 * 2. 上下文转换：在 Effektif 上下文和动态节点上下文之间进行转换
 * 3. 异步处理：处理动态节点的异步执行模式
 * 4. 错误处理：统一处理执行错误和异常情况
 * 5. 状态管理：管理活动实例的状态变化
 * 
 * 设计模式：
 * - 适配器模式：适配不同的接口
 * - 策略模式：支持不同类型的动态节点
 * - 模板方法模式：定义统一的执行流程
 * 
 * 执行流程：
 * 1. 创建节点执行上下文
 * 2. 调用动态节点的execute方法
 * 3. 处理异步执行结果
 * 4. 设置输出变量
 * 5. 推进工作流执行
 * 
 * 使用示例：
 * <pre>{@code
 * // 创建动态节点
 * DynamicNode httpNode = new HttpRequestActionNode();
 * 
 * // 创建节点配置
 * NodeConfig config = NodeConfig.builder()
 *     .put("url", "https://api.example.com/data")
 *     .put("method", "GET")
 *     .build();
 * 
 * // 创建适配器
 * DynamicActivityType activityType = new DynamicActivityType(httpNode, config);
 * 
 * // 注册到工作流引擎
 * workflowEngine.registerActivityType(activityType);
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
public class DynamicActivityType extends AbstractActivityType<DynamicActivity> {
    
    private static final Logger log = LoggerFactory.getLogger(DynamicActivityType.class);
    
    /** 动态节点实例 */
    private final DynamicNode dynamicNode;
    
    /** 节点配置 */
    private final NodeConfig nodeConfig;
    
    /** 活动描述符 */
    private final ActivityDescriptor activityDescriptor;
    
    /**
     * 构造函数
     * 
     * @param dynamicNode 动态节点实例
     * @param nodeConfig 节点配置
     */
    public DynamicActivityType(DynamicNode dynamicNode, NodeConfig nodeConfig) {
        this.dynamicNode = dynamicNode;
        this.nodeConfig = nodeConfig;
        this.activityDescriptor = createActivityDescriptor();
        
        // 初始化动态节点
        try {
            dynamicNode.initialize(nodeConfig);
            log.info("动态活动类型初始化成功，节点类型: {}, 配置: {}", 
                    dynamicNode.getNodeType(), nodeConfig.getKey());
        } catch (Exception e) {
            log.error("动态活动类型初始化失败", e);
            throw new RuntimeException("动态活动类型初始化失败", e);
        }
    }
    
    @Override
    public ActivityDescriptor getDescriptor() {
        return activityDescriptor;
    }
    
    @Override
    public void execute(ActivityInstanceImpl activityInstance) {
        log.debug("开始执行动态活动，实例ID: {}, 节点类型: {}", 
                 activityInstance.getId(), dynamicNode.getNodeType());
        
        try {
            // 1. 创建节点执行上下文
            NodeContext context = createNodeContext(activityInstance);
            
            // 2. 执行动态节点
            CompletableFuture<NodeResult> future = dynamicNode.execute(context);
            
            // 3. 处理执行结果
            handleNodeExecution(activityInstance, future);
            
        } catch (Exception e) {
            log.error("动态活动执行失败，实例ID: {}", activityInstance.getId(), e);
            handleExecutionError(activityInstance, e);
        }
    }
    
    @Override
    public boolean isAsync(ActivityInstanceImpl activityInstance) {
        // 动态节点默认支持异步执行
        return true;
    }
    
    /**
     * 创建节点执行上下文
     * 
     * 将 Effektif 的活动实例上下文转换为动态节点的执行上下文，
     * 包括输入数据、变量、配置等信息。
     * 
     * @param activityInstance Effektif 活动实例
     * @return 节点执行上下文
     */
    private NodeContext createNodeContext(ActivityInstanceImpl activityInstance) {
        // 获取输入数据
        Object inputData = extractInputData(activityInstance);
        
        // 获取工作流变量
        Map<String, Object> variables = extractVariables(activityInstance);
        
        // 获取节点数据（前一个节点的输出）
        Map<String, Object> nodeData = extractNodeData(activityInstance);
        
        // 创建上下文
        return NodeContext.builder()
                .activityInstance(activityInstance)
                .workflowInstance(activityInstance.getWorkflowInstance())
                .inputData(inputData)
                .variables(variables)
                .nodeData(nodeData)
                .nodeConfig(nodeConfig)
                .build();
    }
    
    /**
     * 处理节点执行
     * 
     * 处理动态节点的异步执行，根据节点类型采用不同的处理策略。
     * 
     * @param activityInstance 活动实例
     * @param future 异步执行结果
     */
    private void handleNodeExecution(ActivityInstanceImpl activityInstance, 
                                   CompletableFuture<NodeResult> future) {
        
        if (dynamicNode.getNodeType() == DynamicNode.NodeType.TRIGGER) {
            // 触发节点的特殊处理
            handleTriggerNodeExecution(activityInstance, future);
        } else {
            // 动作节点的标准处理
            handleActionNodeExecution(activityInstance, future);
        }
    }
    
    /**
     * 处理触发节点执行
     * 
     * 触发节点通常是工作流的起始点，需要特殊的处理逻辑。
     * 
     * @param activityInstance 活动实例
     * @param future 异步执行结果
     */
    private void handleTriggerNodeExecution(ActivityInstanceImpl activityInstance, 
                                          CompletableFuture<NodeResult> future) {
        
        future.thenAccept(result -> {
            try {
                if (result.isSuccess()) {
                    log.debug("触发节点执行成功，实例ID: {}", activityInstance.getId());
                    
                    // 处理触发数据
                    Object data = result.getData();
                    if (data != null) {
                        // 设置输出变量
                        setOutputVariables(activityInstance, result);
                        
                        // 继续执行下一个活动
                        activityInstance.onwards();
                    } else {
                        // 没有数据，可能需要等待下次触发
                        log.debug("触发节点没有返回数据，保持等待状态");
                        activityInstance.setWorkState("waiting");
                    }
                } else {
                    // 处理执行错误
                    handleExecutionError(activityInstance, result.getError());
                }
            } catch (Exception e) {
                log.error("处理触发节点执行结果时发生错误", e);
                handleExecutionError(activityInstance, e);
            }
        }).exceptionally(throwable -> {
            log.error("触发节点执行异常", throwable);
            handleExecutionError(activityInstance, throwable);
            return null;
        });
    }
    
    /**
     * 处理动作节点执行
     * 
     * 动作节点是工作流的执行单元，执行完成后需要推进到下一个节点。
     * 
     * @param activityInstance 活动实例
     * @param future 异步执行结果
     */
    private void handleActionNodeExecution(ActivityInstanceImpl activityInstance, 
                                         CompletableFuture<NodeResult> future) {
        
        future.thenAccept(result -> {
            try {
                if (result.isSuccess()) {
                    log.debug("动作节点执行成功，实例ID: {}", activityInstance.getId());
                    
                    // 设置输出变量
                    setOutputVariables(activityInstance, result);
                    
                    // 继续执行下一个活动
                    activityInstance.onwards();
                } else {
                    // 处理执行错误
                    handleExecutionError(activityInstance, result.getError());
                }
            } catch (Exception e) {
                log.error("处理动作节点执行结果时发生错误", e);
                handleExecutionError(activityInstance, e);
            }
        }).exceptionally(throwable -> {
            log.error("动作节点执行异常", throwable);
            handleExecutionError(activityInstance, throwable);
            return null;
        });
    }
    
    /**
     * 设置输出变量
     * 
     * 将节点执行结果设置为活动实例的输出变量，供后续节点使用。
     * 
     * @param activityInstance 活动实例
     * @param result 节点执行结果
     */
    private void setOutputVariables(ActivityInstanceImpl activityInstance, NodeResult result) {
        Object data = result.getData();
        
        if (data != null) {
            // 设置主要输出数据
            activityInstance.setVariableValue("output", data);
            
            // 如果数据是Map，设置各个字段为独立变量
            if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                dataMap.forEach((key, value) -> {
                    if (key instanceof String) {
                        activityInstance.setVariableValue((String) key, value);
                    }
                });
            }
        }
        
        // 设置元数据
        Map<String, Object> metadata = result.getMetadata();
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                activityInstance.setVariableValue("meta_" + key, value);
            });
        }
        
        // 设置执行统计信息
        activityInstance.setVariableValue("executionTime", result.getExecutionTime());
        activityInstance.setVariableValue("nodeType", dynamicNode.getNodeType().getCode());
    }
    
    /**
     * 处理执行错误
     * 
     * 统一处理节点执行过程中的错误和异常。
     * 
     * @param activityInstance 活动实例
     * @param error 错误或异常
     */
    private void handleExecutionError(ActivityInstanceImpl activityInstance, Throwable error) {
        log.error("动态节点执行失败，实例ID: {}, 错误: {}", 
                 activityInstance.getId(), error.getMessage(), error);
        
        // 设置错误信息
        activityInstance.setException(error);
        
        // 设置错误变量
        activityInstance.setVariableValue("error", error.getMessage());
        activityInstance.setVariableValue("errorType", error.getClass().getSimpleName());
        
        // 结束活动实例
        activityInstance.end();
    }
    
    /**
     * 提取输入数据
     * 
     * 从活动实例中提取输入数据，包括直接输入和变量引用。
     * 
     * @param activityInstance 活动实例
     * @return 输入数据
     */
    private Object extractInputData(ActivityInstanceImpl activityInstance) {
        // 尝试从多个来源获取输入数据
        Object inputData = null;
        
        // 1. 从活动配置的输入参数获取
        if (activityInstance.getActivity().getInputs() != null) {
            Map<String, Object> inputs = new HashMap<>();
            activityInstance.getActivity().getInputs().forEach((key, inputParam) -> {
                Object value = activityInstance.getValue(inputParam);
                if (value != null) {
                    inputs.put(key, value);
                }
            });
            if (!inputs.isEmpty()) {
                inputData = inputs;
            }
        }
        
        // 2. 从工作流实例的触发数据获取
        if (inputData == null) {
            inputData = activityInstance.getWorkflowInstance().getTriggerInstance().getData();
        }
        
        // 3. 从前一个活动的输出获取
        if (inputData == null) {
            inputData = activityInstance.getVariableValue("input");
        }
        
        return inputData;
    }
    
    /**
     * 提取工作流变量
     * 
     * @param activityInstance 活动实例
     * @return 变量映射
     */
    private Map<String, Object> extractVariables(ActivityInstanceImpl activityInstance) {
        Map<String, Object> variables = new HashMap<>();
        
        // 获取工作流实例的所有变量
        if (activityInstance.getWorkflowInstance().getVariableValues() != null) {
            variables.putAll(activityInstance.getWorkflowInstance().getVariableValues());
        }
        
        // 获取活动实例的局部变量
        if (activityInstance.getVariableValues() != null) {
            variables.putAll(activityInstance.getVariableValues());
        }
        
        return variables;
    }
    
    /**
     * 提取节点数据
     * 
     * @param activityInstance 活动实例
     * @return 节点数据映射
     */
    private Map<String, Object> extractNodeData(ActivityInstanceImpl activityInstance) {
        Map<String, Object> nodeData = new HashMap<>();
        
        // 获取前一个节点的输出数据
        Object previousOutput = activityInstance.getVariableValue("output");
        if (previousOutput != null) {
            nodeData.put("previous", previousOutput);
        }
        
        // 获取当前节点的配置
        nodeData.put("config", nodeConfig.toMap());
        
        return nodeData;
    }
    
    /**
     * 创建活动描述符
     * 
     * 基于动态节点的描述符创建 Effektif 的活动描述符。
     * 
     * @return 活动描述符
     */
    private ActivityDescriptor createActivityDescriptor() {
        com.effektif.dynamic.node.NodeDescriptor nodeDescriptor = dynamicNode.getDescriptor();
        
        return ActivityDescriptor.builder()
                .activityApiClass(DynamicActivity.class)
                .activityImplClass(this.getClass())
                .name(nodeDescriptor.getName())
                .description(nodeDescriptor.getDescription())
                .category(nodeDescriptor.getCategory())
                .build();
    }
    
    /**
     * 获取动态节点实例
     * 
     * @return 动态节点实例
     */
    public DynamicNode getDynamicNode() {
        return dynamicNode;
    }
    
    /**
     * 获取节点配置
     * 
     * @return 节点配置
     */
    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }
}
