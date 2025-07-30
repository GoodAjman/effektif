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
 * 动作节点接口
 * 
 * 动作节点负责执行具体的业务操作，是工作流中的执行单元。
 * 这个接口扩展了 DynamicNode，添加了动作节点特有的功能。
 * 
 * 设计理念：
 * 1. 单一职责：每个动作节点专注于一种特定的操作
 * 2. 可组合性：动作节点可以串联组成复杂的工作流
 * 3. 批量支持：支持批量操作以提高效率
 * 4. 错误处理：完善的错误处理和重试机制
 * 5. 幂等性：支持幂等操作，确保重试安全
 * 
 * 典型使用场景：
 * - HTTP 请求：调用外部 API 接口
 * - 数据库操作：查询、插入、更新数据
 * - 文件操作：上传、下载、转换文件
 * - 邮件发送：发送通知邮件
 * - 消息发送：发送短信、推送通知
 * - 数据转换：格式转换、字段映射
 * 
 * 使用示例：
 * <pre>{@code
 * // 创建 HTTP 请求动作节点
 * ActionNode httpAction = new HttpRequestActionNode();
 * 
 * // 配置动作节点
 * NodeConfig config = NodeConfig.builder()
 *     .put("url", "https://api.example.com/users")
 *     .put("method", "POST")
 *     .put("headers", Map.of("Content-Type", "application/json"))
 *     .put("body", "{{$json}}")
 *     .build();
 * 
 * httpAction.initialize(config);
 * 
 * // 执行动作
 * ActionContext context = ActionContext.builder()
 *     .inputData(userData)
 *     .variables(workflowVariables)
 *     .build();
 * 
 * ActionResult result = httpAction.executeAction(context);
 * if (result.isSuccess()) {
 *     // 处理成功结果
 *     Object responseData = result.getData();
 * } else {
 *     // 处理错误
 *     Exception error = result.getError();
 * }
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
public interface ActionNode extends DynamicNode {
    
    /**
     * 动作类型枚举
     * 
     * 定义了系统支持的动作类型，每种类型对应特定的操作领域：
     * - HTTP_REQUEST: HTTP请求操作
     * - DATABASE_QUERY: 数据库查询操作
     * - DATABASE_UPDATE: 数据库更新操作
     * - FILE_OPERATION: 文件操作
     * - EMAIL_SEND: 邮件发送
     * - MESSAGE_SEND: 消息发送
     * - DATA_TRANSFORM: 数据转换
     * - SCRIPT_EXECUTE: 脚本执行
     * - CUSTOM: 自定义动作
     */
    enum ActionType {
        /** HTTP请求 - 调用外部API接口 */
        HTTP_REQUEST("http_request", "HTTP请求", 
                    "发起HTTP请求调用外部API，支持GET、POST、PUT、DELETE等方法"),
        
        /** 数据库查询 - 查询数据库数据 */
        DATABASE_QUERY("db_query", "数据库查询", 
                      "执行数据库查询操作，支持SQL查询和NoSQL查询"),
        
        /** 数据库更新 - 更新数据库数据 */
        DATABASE_UPDATE("db_update", "数据库更新", 
                       "执行数据库更新操作，包括插入、更新、删除等"),
        
        /** 文件操作 - 文件上传下载等 */
        FILE_OPERATION("file_op", "文件操作", 
                      "处理文件操作，包括上传、下载、转换、压缩等"),
        
        /** 邮件发送 - 发送邮件通知 */
        EMAIL_SEND("email_send", "邮件发送", 
                  "发送邮件通知，支持HTML邮件、附件、模板等"),
        
        /** 消息发送 - 发送短信、推送等 */
        MESSAGE_SEND("message_send", "消息发送", 
                    "发送各种消息通知，包括短信、推送、即时消息等"),
        
        /** 数据转换 - 数据格式转换 */
        DATA_TRANSFORM("data_transform", "数据转换", 
                      "执行数据格式转换、字段映射、数据清洗等操作"),
        
        /** 脚本执行 - 执行自定义脚本 */
        SCRIPT_EXECUTE("script_execute", "脚本执行", 
                      "执行JavaScript、Python等脚本代码"),
        
        /** 自定义动作 - 用户自定义的动作类型 */
        CUSTOM("custom", "自定义动作", 
              "用户自定义的动作类型，支持扩展业务逻辑");
        
        private final String code;
        private final String name;
        private final String description;
        
        ActionType(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * 执行策略枚举
     * 
     * 定义了动作节点的执行策略：
     * - SYNC: 同步执行，阻塞等待结果
     * - ASYNC: 异步执行，立即返回
     * - RETRY: 支持重试的执行
     * - IDEMPOTENT: 幂等执行，可安全重试
     */
    enum ExecutionStrategy {
        /** 同步执行 */
        SYNC("sync", "同步执行"),
        
        /** 异步执行 */
        ASYNC("async", "异步执行"),
        
        /** 支持重试 */
        RETRY("retry", "重试执行"),
        
        /** 幂等执行 */
        IDEMPOTENT("idempotent", "幂等执行");
        
        private final String code;
        private final String name;
        
        ExecutionStrategy(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
    }
    
    /**
     * 执行动作
     * 
     * 这是动作节点的核心执行方法，实现具体的业务逻辑。
     * 方法接收动作上下文，执行相应的操作，并返回执行结果。
     * 
     * 执行流程：
     * 1. 从上下文中获取输入数据和配置参数
     * 2. 解析表达式和变量引用
     * 3. 执行具体的业务操作
     * 4. 处理执行结果或异常
     * 5. 返回标准化的执行结果
     * 
     * 错误处理：
     * - 捕获并包装所有异常
     * - 提供详细的错误信息
     * - 支持错误重试和降级处理
     * 
     * @param context 动作执行上下文，包含输入数据、变量、配置等
     * @return 动作执行结果，包含输出数据或错误信息
     * @throws IllegalArgumentException 如果上下文参数无效
     */
    ActionResult executeAction(ActionContext context);
    
    /**
     * 获取动作类型
     * 
     * @return 动作类型枚举值
     */
    ActionType getActionType();
    
    /**
     * 是否支持批量操作
     * 
     * 返回该动作节点是否支持批量操作。支持批量操作的节点
     * 可以在一次调用中处理多个数据项，提高执行效率。
     * 
     * @return true 如果支持批量操作，false 否则
     */
    boolean supportsBatchOperation();
    
    /**
     * 批量执行动作
     * 
     * 批量执行多个动作，适用于需要批量处理数据的场景。
     * 这个方法只有在 supportsBatchOperation() 返回 true 时才有效。
     * 
     * 批量执行的优势：
     * 1. 减少网络开销：一次请求处理多个数据项
     * 2. 提高吞吐量：批量操作通常比单个操作更高效
     * 3. 事务支持：可以在一个事务中处理多个操作
     * 
     * 实现注意事项：
     * - 需要处理部分成功的情况
     * - 提供详细的批量执行结果
     * - 支持批量大小限制
     * 
     * @param contexts 动作上下文列表，每个上下文对应一个数据项
     * @return 动作执行结果列表，与输入上下文一一对应
     * @throws UnsupportedOperationException 如果不支持批量操作
     * @throws IllegalArgumentException 如果上下文列表为空或过大
     */
    List<ActionResult> executeBatchAction(List<ActionContext> contexts);
    
    /**
     * 获取执行策略
     * 
     * 返回动作节点的执行策略，用于确定如何执行该动作。
     * 
     * @return 执行策略枚举值
     */
    default ExecutionStrategy getExecutionStrategy() {
        return ExecutionStrategy.SYNC;
    }
    
    /**
     * 是否支持重试
     * 
     * 返回该动作是否支持重试。支持重试的动作在执行失败时
     * 可以自动重试，提高执行成功率。
     * 
     * @return true 如果支持重试，false 否则
     */
    default boolean supportsRetry() {
        return false;
    }
    
    /**
     * 是否为幂等操作
     * 
     * 返回该动作是否为幂等操作。幂等操作可以安全地重复执行，
     * 不会产生副作用。
     * 
     * @return true 如果是幂等操作，false 否则
     */
    default boolean isIdempotent() {
        return false;
    }
    
    /**
     * 获取最大批量大小
     * 
     * 返回批量操作支持的最大数据项数量。
     * 
     * @return 最大批量大小，如果不支持批量操作则返回1
     */
    default int getMaxBatchSize() {
        return supportsBatchOperation() ? 100 : 1;
    }
    
    /**
     * 预估执行时间
     * 
     * 预估动作执行所需的时间（毫秒），用于调度和超时控制。
     * 
     * @param context 动作执行上下文
     * @return 预估执行时间（毫秒）
     */
    default long estimateExecutionTime(ActionContext context) {
        return 5000; // 默认5秒
    }
    
    /**
     * 默认实现：动作节点的节点类型固定为 ACTION
     */
    @Override
    default NodeType getNodeType() {
        return NodeType.ACTION;
    }
}
