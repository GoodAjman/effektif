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
 * 动态节点基础接口
 * 
 * 这是动态节点系统的核心接口，参考了 n8n 和 Zapier 的设计理念，
 * 提供了统一的节点抽象，支持多种类型的节点实现。
 * 
 * 设计特点：
 * 1. 统一的节点接口，支持触发器、动作、转换等多种节点类型
 * 2. 异步执行模型，支持高并发处理
 * 3. 灵活的配置系统，支持动态配置和验证
 * 4. 完善的错误处理和状态管理
 * 
 * 使用示例：
 * <pre>{@code
 * // 创建 HTTP 触发节点
 * DynamicNode httpTrigger = new HttpTriggerNode();
 * httpTrigger.initialize(nodeConfig);
 * 
 * // 执行节点
 * CompletableFuture<NodeResult> future = httpTrigger.execute(context);
 * future.thenAccept(result -> {
 *     if (result.isSuccess()) {
 *         // 处理成功结果
 *         processResult(result.getData());
 *     } else {
 *         // 处理错误
 *         handleError(result.getError());
 *     }
 * });
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
public interface DynamicNode {
    
    /**
     * 节点类型枚举
     * 
     * 定义了系统支持的所有节点类型，每种类型都有特定的执行语义：
     * - TRIGGER: 触发节点，负责从外部系统获取数据并启动工作流
     * - ACTION: 动作节点，执行具体的业务操作
     * - TRANSFORM: 转换节点，对数据进行格式转换和处理
     * - CONDITION: 条件节点，根据条件进行流程分支
     * - WEBHOOK: Webhook节点，处理HTTP回调
     * - HTTP: HTTP请求节点，发起HTTP调用
     * - DATABASE: 数据库节点，执行数据库操作
     * - FILE: 文件处理节点，处理文件上传下载等操作
     */
    enum NodeType {
        /** 触发节点 - 工作流的起始点，负责数据拉取和流程启动 */
        TRIGGER("trigger", "触发节点", "从外部系统获取数据并启动工作流"),
        
        /** 动作节点 - 执行具体业务操作 */
        ACTION("action", "动作节点", "执行具体的业务操作，如API调用、数据处理等"),
        
        /** 转换节点 - 数据格式转换 */
        TRANSFORM("transform", "转换节点", "对数据进行格式转换、字段映射等处理"),
        
        /** 条件节点 - 流程分支控制 */
        CONDITION("condition", "条件节点", "根据条件表达式进行流程分支控制"),
        
        /** Webhook节点 - HTTP回调处理 */
        WEBHOOK("webhook", "Webhook节点", "处理HTTP回调请求"),
        
        /** HTTP请求节点 - 发起HTTP调用 */
        HTTP("http", "HTTP节点", "发起HTTP请求调用外部API"),
        
        /** 数据库节点 - 数据库操作 */
        DATABASE("database", "数据库节点", "执行数据库查询、更新等操作"),
        
        /** 文件处理节点 - 文件操作 */
        FILE("file", "文件节点", "处理文件上传、下载、转换等操作");
        
        private final String code;
        private final String name;
        private final String description;
        
        NodeType(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * 数据处理策略枚举
     * 
     * 定义了节点处理数据的不同策略，影响节点的执行行为：
     * - SINGLE_ITEM: 单条数据处理，适用于简单的一对一处理场景
     * - BATCH_PROCESS: 批量处理，适用于需要批量操作的场景
     * - STREAM_PROCESS: 流式处理，适用于大数据量的实时处理场景
     */
    enum DataStrategy {
        /** 单条数据处理 - 逐条处理数据，适合简单场景 */
        SINGLE_ITEM("single", "单条处理", "逐条处理数据，适合简单的一对一处理场景"),
        
        /** 批量处理 - 批量处理数据，提高效率 */
        BATCH_PROCESS("batch", "批量处理", "批量处理数据，适合需要批量操作的场景"),
        
        /** 流式处理 - 流式处理大数据量 */
        STREAM_PROCESS("stream", "流式处理", "流式处理数据，适合大数据量的实时处理场景");
        
        private final String code;
        private final String name;
        private final String description;
        
        DataStrategy(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * 获取节点类型
     * 
     * @return 节点类型枚举值
     */
    NodeType getNodeType();
    
    /**
     * 获取节点描述符
     * 
     * 节点描述符包含了节点的元数据信息，用于UI配置界面的渲染，
     * 包括节点名称、描述、输入输出参数定义、配置项等。
     * 
     * @return 节点描述符对象，包含节点的元数据信息
     */
    NodeDescriptor getDescriptor();
    
    /**
     * 初始化节点
     * 
     * 在节点开始执行前调用，用于初始化节点的配置和状态。
     * 这个方法应该验证配置的有效性，初始化必要的资源。
     * 
     * @param config 节点配置对象，包含节点的所有配置参数
     * @throws IllegalArgumentException 如果配置无效
     * @throws RuntimeException 如果初始化失败
     */
    void initialize(NodeConfig config);
    
    /**
     * 执行节点逻辑
     * 
     * 这是节点的核心执行方法，实现节点的具体业务逻辑。
     * 方法采用异步设计，返回 CompletableFuture 以支持非阻塞执行。
     * 
     * 执行流程：
     * 1. 从上下文中获取输入数据
     * 2. 执行节点的具体业务逻辑
     * 3. 处理执行结果或异常
     * 4. 返回执行结果
     * 
     * @param context 节点执行上下文，包含输入数据、变量、配置等信息
     * @return CompletableFuture<NodeResult> 异步执行结果
     */
    CompletableFuture<NodeResult> execute(NodeContext context);
    
    /**
     * 验证节点配置
     * 
     * 验证节点配置的有效性，包括必填参数检查、格式验证、
     * 依赖检查等。这个方法在节点初始化前调用。
     * 
     * @param config 待验证的节点配置
     * @return 验证结果，包含验证状态和错误信息
     */
    ValidationResult validate(NodeConfig config);
    
    /**
     * 获取支持的数据策略
     * 
     * 返回节点支持的数据处理策略列表。不同的节点可能支持
     * 不同的数据处理策略，调用方可以根据需要选择合适的策略。
     * 
     * @return 支持的数据策略列表
     */
    List<DataStrategy> getSupportedDataStrategies();
    
    /**
     * 获取节点状态
     * 
     * 返回节点的当前状态，用于监控和调试。
     * 
     * @return 节点状态对象
     */
    default NodeStatus getStatus() {
        return NodeStatus.READY;
    }
    
    /**
     * 清理资源
     * 
     * 在节点销毁时调用，用于清理节点占用的资源，
     * 如关闭连接、释放内存等。
     */
    default void cleanup() {
        // 默认实现为空，子类可以重写
    }
}
