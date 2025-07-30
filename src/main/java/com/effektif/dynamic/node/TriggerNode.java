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
package com.effektif.dynamic.node;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 触发节点接口
 * 
 * 触发节点是工作流的起始点，负责从外部系统拉取或接收数据，
 * 并启动相应的工作流实例。这个接口扩展了 DynamicNode，
 * 添加了触发器特有的功能。
 * 
 * 设计理念：
 * 1. 支持多种触发策略：轮询、推送、定时、事件驱动
 * 2. 自动数据拆分：类似 Zapier 的数据处理方式
 * 3. 状态管理：支持触发器的启动、停止、状态查询
 * 4. 错误处理：完善的错误处理和重试机制
 * 
 * 典型使用场景：
 * - HTTP API 轮询：定期从 REST API 拉取新数据
 * - Webhook 接收：接收第三方系统的推送数据
 * - 定时任务：按照时间规则定时触发工作流
 * - 文件监听：监听文件系统变化触发处理
 * - 消息队列：监听消息队列中的新消息
 * 
 * 使用示例：
 * <pre>{@code
 * // 创建 HTTP 轮询触发器
 * TriggerNode httpTrigger = new HttpPollingTriggerNode();
 * 
 * // 配置触发器
 * NodeConfig config = NodeConfig.builder()
 *     .put("apiUrl", "https://api.example.com/data")
 *     .put("pollInterval", 60)
 *     .put("method", "GET")
 *     .build();
 * 
 * httpTrigger.initialize(config);
 * 
 * // 启动触发器
 * httpTrigger.startTrigger().thenRun(() -> {
 *     System.out.println("触发器已启动");
 * });
 * 
 * // 处理触发的数据
 * TriggerContext context = TriggerContext.builder()
 *     .workflowId("workflow-123")
 *     .triggerId("trigger-456")
 *     .build();
 * 
 * List<WorkflowExecution> executions = httpTrigger.processTriggerData(data, context);
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
public interface TriggerNode extends DynamicNode {
    
    /**
     * 触发策略枚举
     * 
     * 定义了触发器获取数据的不同策略，每种策略适用于不同的场景：
     * - POLLING: 轮询拉取，主动从外部系统定期获取数据
     * - WEBHOOK: Webhook推送，被动接收外部系统推送的数据
     * - SCHEDULED: 定时触发，按照时间规则定时执行
     * - EVENT_DRIVEN: 事件驱动，响应特定事件触发
     */
    enum TriggerStrategy {
        /** 轮询拉取策略 - 主动定期从外部系统获取数据 */
        POLLING("polling", "轮询拉取", 
                "主动定期从外部系统获取数据，适用于 REST API、数据库查询等场景"),
        
        /** Webhook推送策略 - 被动接收外部系统推送的数据 */
        WEBHOOK("webhook", "Webhook推送", 
                "被动接收外部系统推送的数据，适用于实时通知、回调等场景"),
        
        /** 定时触发策略 - 按照时间规则定时执行 */
        SCHEDULED("scheduled", "定时触发", 
                "按照 Cron 表达式或固定间隔定时执行，适用于定时任务场景"),
        
        /** 事件驱动策略 - 响应特定事件触发 */
        EVENT_DRIVEN("event", "事件驱动", 
                "响应文件变化、消息队列等事件触发，适用于实时处理场景");
        
        private final String code;
        private final String name;
        private final String description;
        
        TriggerStrategy(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * 触发器状态枚举
     * 
     * 表示触发器的当前运行状态：
     * - STOPPED: 已停止，触发器未运行
     * - STARTING: 启动中，正在初始化触发器
     * - RUNNING: 运行中，触发器正常工作
     * - STOPPING: 停止中，正在关闭触发器
     * - ERROR: 错误状态，触发器遇到错误
     */
    enum TriggerStatus {
        /** 已停止 */
        STOPPED("stopped", "已停止"),
        
        /** 启动中 */
        STARTING("starting", "启动中"),
        
        /** 运行中 */
        RUNNING("running", "运行中"),
        
        /** 停止中 */
        STOPPING("stopping", "停止中"),
        
        /** 错误状态 */
        ERROR("error", "错误状态");
        
        private final String code;
        private final String name;
        
        TriggerStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
    }
    
    /**
     * 开始监听/拉取数据
     * 
     * 启动触发器，开始监听外部事件或定期拉取数据。
     * 这个方法是异步的，返回 CompletableFuture 以支持非阻塞启动。
     * 
     * 启动过程：
     * 1. 验证触发器配置
     * 2. 初始化必要的资源（连接、线程池等）
     * 3. 开始监听或调度任务
     * 4. 更新触发器状态为 RUNNING
     * 
     * @return CompletableFuture<Void> 启动完成的异步结果
     * @throws IllegalStateException 如果触发器已经在运行
     * @throws RuntimeException 如果启动失败
     */
    CompletableFuture<Void> startTrigger();
    
    /**
     * 停止监听/拉取数据
     * 
     * 停止触发器，清理相关资源。这个方法应该优雅地关闭触发器，
     * 等待当前正在处理的任务完成，然后释放资源。
     * 
     * 停止过程：
     * 1. 设置停止标志，不再接受新的触发
     * 2. 等待当前正在处理的任务完成
     * 3. 关闭连接、线程池等资源
     * 4. 更新触发器状态为 STOPPED
     * 
     * @throws RuntimeException 如果停止过程中发生错误
     */
    void stopTrigger();
    
    /**
     * 获取触发策略
     * 
     * @return 触发器使用的策略
     */
    TriggerStrategy getTriggerStrategy();
    
    /**
     * 处理触发的数据
     * 
     * 这是触发器的核心处理方法，负责处理从外部系统获取的数据，
     * 并启动相应的工作流实例。支持数据拆分和批量处理。
     * 
     * 处理流程：
     * 1. 验证触发数据的格式和有效性
     * 2. 根据配置决定是否拆分数据（类似 Zapier 的处理方式）
     * 3. 为每个数据项创建工作流执行实例
     * 4. 设置执行上下文和变量
     * 5. 启动工作流实例
     * 6. 返回执行结果列表
     * 
     * 数据拆分策略：
     * - 如果数据是数组，自动拆分为单个元素
     * - 如果数据是单个对象，直接处理
     * - 支持自定义拆分路径（如 $.data.items）
     * 
     * @param data 触发的原始数据，可以是单个对象或数组
     * @param context 触发上下文，包含工作流ID、触发器ID等信息
     * @return 工作流执行实例列表，每个数据项对应一个执行实例
     * @throws IllegalArgumentException 如果数据格式无效
     * @throws RuntimeException 如果处理过程中发生错误
     */
    List<WorkflowExecution> processTriggerData(Object data, TriggerContext context);
    
    /**
     * 获取触发状态
     * 
     * 返回触发器的当前状态，用于监控和调试。
     * 
     * @return 触发器状态
     */
    TriggerStatus getTriggerStatus();
    
    /**
     * 获取触发统计信息
     * 
     * 返回触发器的统计信息，包括触发次数、成功次数、失败次数等。
     * 这些信息用于监控和性能分析。
     * 
     * @return 触发统计信息
     */
    default TriggerStatistics getStatistics() {
        return TriggerStatistics.empty();
    }
    
    /**
     * 检查触发器健康状态
     * 
     * 检查触发器是否正常工作，包括连接状态、资源使用情况等。
     * 
     * @return 健康检查结果
     */
    default HealthCheckResult checkHealth() {
        return HealthCheckResult.healthy();
    }
    
    /**
     * 重置触发器状态
     * 
     * 重置触发器的内部状态，清除错误信息和统计数据。
     * 通常在触发器遇到错误后调用，以便重新开始正常工作。
     */
    default void reset() {
        // 默认实现为空，子类可以重写
    }
    
    /**
     * 默认实现：触发节点的节点类型固定为 TRIGGER
     */
    @Override
    default NodeType getNodeType() {
        return NodeType.TRIGGER;
    }
}
