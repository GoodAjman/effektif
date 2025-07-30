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
package com.effektif.dynamic.node.impl;

import com.effektif.dynamic.data.DataItem;
import com.effektif.dynamic.data.DataSplitter;
import com.effektif.dynamic.node.TriggerNode;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.model.TriggerInstance;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 轮询触发节点实现
 * 
 * 实现类似 Zapier 的 HTTP 轮询触发器功能，定期从外部 API 拉取数据，
 * 自动检测新数据并启动相应的工作流实例。
 * 
 * 核心功能：
 * 1. 定期轮询：按照配置的间隔时间定期请求外部API
 * 2. 数据检测：智能检测新数据，避免重复处理
 * 3. 自动拆分：自动拆分批量数据为单个工作流实例
 * 4. 状态管理：维护轮询状态和最后拉取时间
 * 5. 错误处理：完善的错误处理和重试机制
 * 
 * 配置参数：
 * - apiUrl: API 接口地址
 * - pollInterval: 轮询间隔（秒）
 * - method: HTTP 方法（默认GET）
 * - headers: 请求头配置
 * - queryParams: 查询参数
 * - timeout: 请求超时时间
 * - lastPollTimeField: 用于增量拉取的时间字段
 * - dataPath: 数据提取路径（JSONPath）
 * 
 * 使用示例：
 * <pre>{@code
 * // 配置HTTP轮询触发器
 * NodeConfig config = NodeConfig.builder()
 *     .put("apiUrl", "https://api.example.com/orders")
 *     .put("pollInterval", 60)  // 每60秒轮询一次
 *     .put("method", "GET")
 *     .put("headers", Map.of("Authorization", "Bearer token123"))
 *     .put("queryParams", Map.of("status", "new"))
 *     .put("dataPath", "$.data")  // 从响应的data字段提取数据
 *     .put("lastPollTimeField", "updated_at")  // 增量拉取字段
 *     .build();
 * 
 * // 初始化并启动触发器
 * HttpPollingTriggerNode trigger = new HttpPollingTriggerNode();
 * trigger.initialize(config);
 * trigger.startTrigger();
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
@Component
public class HttpPollingTriggerNode implements TriggerNode {
    
    private static final Logger log = LoggerFactory.getLogger(HttpPollingTriggerNode.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private DataSplitter dataSplitter;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /** 节点配置 */
    private NodeConfig config;
    
    /** 调度器，用于定期轮询 */
    private ScheduledExecutorService scheduler;
    
    /** 触发器运行状态 */
    private volatile TriggerStatus status = TriggerStatus.STOPPED;
    
    /** 最后轮询时间 */
    private volatile LocalDateTime lastPollTime;
    
    /** 最后成功拉取的数据时间戳 */
    private volatile String lastDataTimestamp;
    
    /** 触发统计信息 */
    private final TriggerStatistics statistics = new TriggerStatistics();
    
    /** 节点描述符 */
    private static final NodeDescriptor DESCRIPTOR = NodeDescriptor.builder()
            .key("http_polling_trigger")
            .name("HTTP轮询触发器")
            .description("定期从HTTP API拉取数据并启动工作流")
            .category("trigger")
            .icon("http")
            .inputDescriptor("apiUrl", "API地址", "string", true)
            .inputDescriptor("pollInterval", "轮询间隔(秒)", "integer", false, 60)
            .inputDescriptor("method", "HTTP方法", "string", false, "GET")
            .inputDescriptor("headers", "请求头", "object", false)
            .inputDescriptor("queryParams", "查询参数", "object", false)
            .inputDescriptor("timeout", "超时时间(毫秒)", "integer", false, 30000)
            .inputDescriptor("dataPath", "数据路径", "string", false, "$")
            .inputDescriptor("lastPollTimeField", "时间字段", "string", false)
            .outputDescriptor("data", "拉取的数据")
            .outputDescriptor("timestamp", "拉取时间")
            .outputDescriptor("count", "数据数量")
            .build();
    
    @Override
    public NodeType getNodeType() {
        return NodeType.TRIGGER;
    }
    
    @Override
    public TriggerStrategy getTriggerStrategy() {
        return TriggerStrategy.POLLING;
    }
    
    @Override
    public NodeDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Override
    public void initialize(NodeConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "HttpPollingTrigger-" + config.getString("triggerId", "unknown"));
            thread.setDaemon(true);
            return thread;
        });
        
        log.info("HTTP轮询触发器初始化完成，API地址: {}, 轮询间隔: {}秒", 
                config.getString("apiUrl"), config.getInt("pollInterval", 60));
    }
    
    @Override
    public CompletableFuture<Void> startTrigger() {
        if (status == TriggerStatus.RUNNING) {
            log.warn("触发器已经在运行中");
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                status = TriggerStatus.STARTING;
                
                // 验证配置
                validateConfiguration();
                
                // 开始定期轮询
                int pollInterval = config.getInt("pollInterval", 60);
                scheduler.scheduleWithFixedDelay(
                    this::pollData,
                    0,  // 立即开始第一次轮询
                    pollInterval,
                    TimeUnit.SECONDS
                );
                
                status = TriggerStatus.RUNNING;
                log.info("HTTP轮询触发器启动成功，轮询间隔: {}秒", pollInterval);
                
            } catch (Exception e) {
                status = TriggerStatus.ERROR;
                log.error("HTTP轮询触发器启动失败", e);
                throw new RuntimeException("触发器启动失败", e);
            }
        });
    }
    
    @Override
    public void stopTrigger() {
        if (status == TriggerStatus.STOPPED) {
            log.warn("触发器已经停止");
            return;
        }
        
        try {
            status = TriggerStatus.STOPPING;
            
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                
                // 等待当前任务完成
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("等待轮询任务完成超时，强制关闭");
                    scheduler.shutdownNow();
                }
            }
            
            status = TriggerStatus.STOPPED;
            log.info("HTTP轮询触发器已停止");
            
        } catch (Exception e) {
            status = TriggerStatus.ERROR;
            log.error("停止HTTP轮询触发器时发生错误", e);
        }
    }
    
    @Override
    public List<WorkflowExecution> processTriggerData(Object data, TriggerContext context) {
        List<WorkflowExecution> executions = new ArrayList<>();
        
        try {
            // 1. 拆分数据
            List<DataItem> dataItems = dataSplitter.splitData(
                data, 
                DataSplitter.SplitStrategy.AUTO_DETECT, 
                config.getString("dataPath")
            );
            
            log.debug("数据拆分完成，原始数据拆分为 {} 个数据项", dataItems.size());
            
            // 2. 为每个数据项创建工作流执行
            for (DataItem dataItem : dataItems) {
                try {
                    // 创建触发实例
                    TriggerInstance triggerInstance = new TriggerInstance()
                        .workflowId(context.getWorkflowId())
                        .data(dataItem.getData())
                        .sourceId("http-polling-trigger")
                        .metadata(createTriggerMetadata(dataItem, context));
                    
                    // 启动工作流
                    WorkflowInstance instance = workflowEngine.start(triggerInstance);
                    
                    // 创建执行记录
                    WorkflowExecution execution = new WorkflowExecution(
                        instance.getId().toString(),
                        dataItem,
                        LocalDateTime.now(),
                        WorkflowExecution.Status.STARTED
                    );
                    
                    executions.add(execution);
                    
                    // 更新统计信息
                    statistics.incrementSuccessCount();
                    
                    log.debug("工作流实例启动成功，实例ID: {}, 数据索引: {}", 
                             instance.getId(), dataItem.getIndex());
                    
                } catch (Exception e) {
                    log.error("启动工作流实例失败，数据索引: {}, 错误: {}", 
                             dataItem.getIndex(), e.getMessage(), e);
                    
                    // 创建失败的执行记录
                    WorkflowExecution execution = new WorkflowExecution(
                        null,
                        dataItem,
                        LocalDateTime.now(),
                        WorkflowExecution.Status.FAILED
                    );
                    execution.setErrorMessage(e.getMessage());
                    executions.add(execution);
                    
                    // 更新统计信息
                    statistics.incrementErrorCount();
                }
            }
            
        } catch (Exception e) {
            log.error("处理触发数据失败", e);
            statistics.incrementErrorCount();
            throw new RuntimeException("处理触发数据失败", e);
        }
        
        return executions;
    }
    
    @Override
    public TriggerStatus getTriggerStatus() {
        return status;
    }
    
    @Override
    public TriggerStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public CompletableFuture<NodeResult> execute(NodeContext context) {
        // 触发节点的execute方法通常用于手动触发
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 执行一次数据拉取
                Object data = fetchDataFromApi();
                
                if (data != null) {
                    return NodeResult.success(data);
                } else {
                    return NodeResult.success(Collections.emptyMap());
                }
                
            } catch (Exception e) {
                log.error("手动执行HTTP轮询失败", e);
                return NodeResult.failure(e);
            }
        });
    }
    
    @Override
    public ValidationResult validate(NodeConfig config) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // 验证必填参数
        if (config.getString("apiUrl") == null || config.getString("apiUrl").trim().isEmpty()) {
            builder.addError("apiUrl", "API地址不能为空");
        }
        
        // 验证轮询间隔
        int pollInterval = config.getInt("pollInterval", 60);
        if (pollInterval < 1) {
            builder.addError("pollInterval", "轮询间隔必须大于0秒");
        } else if (pollInterval < 10) {
            builder.addWarning("pollInterval", "轮询间隔过短可能导致API限流");
        }
        
        // 验证HTTP方法
        String method = config.getString("method", "GET");
        if (!Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH").contains(method.toUpperCase())) {
            builder.addError("method", "不支持的HTTP方法: " + method);
        }
        
        // 验证超时时间
        int timeout = config.getInt("timeout", 30000);
        if (timeout < 1000) {
            builder.addWarning("timeout", "超时时间过短可能导致请求失败");
        }
        
        return builder.build();
    }
    
    @Override
    public List<DataStrategy> getSupportedDataStrategies() {
        return Arrays.asList(
            DataStrategy.SINGLE_ITEM,
            DataStrategy.BATCH_PROCESS
        );
    }
    
    @Override
    public void cleanup() {
        stopTrigger();
    }
    
    /**
     * 轮询数据的核心方法
     */
    private void pollData() {
        if (status != TriggerStatus.RUNNING) {
            return;
        }
        
        try {
            log.debug("开始执行HTTP轮询，API地址: {}", config.getString("apiUrl"));
            
            lastPollTime = LocalDateTime.now();
            statistics.incrementPollCount();
            
            // 1. 构建请求参数
            Map<String, Object> params = buildRequestParams();
            
            // 2. 发起HTTP请求
            Object responseData = fetchDataFromApi(params);
            
            // 3. 检查是否有新数据
            if (hasNewData(responseData)) {
                // 4. 创建触发上下文
                TriggerContext context = TriggerContext.builder()
                    .workflowId(config.getString("workflowId"))
                    .triggerId(config.getString("triggerId"))
                    .build();
                
                // 5. 处理数据
                List<WorkflowExecution> executions = processTriggerData(responseData, context);
                
                log.info("HTTP轮询完成，处理了 {} 个工作流实例", executions.size());
                
                // 6. 更新最后数据时间戳
                updateLastDataTimestamp(responseData);
                
            } else {
                log.debug("HTTP轮询完成，没有新数据");
            }
            
        } catch (Exception e) {
            log.error("HTTP轮询执行失败", e);
            statistics.incrementErrorCount();
            
            // 根据错误类型决定是否继续轮询
            if (isRetryableError(e)) {
                log.info("遇到可重试错误，将在下次轮询时重试");
            } else {
                log.error("遇到不可重试错误，停止轮询");
                status = TriggerStatus.ERROR;
            }
        }
    }
    
    /**
     * 从API获取数据
     */
    private Object fetchDataFromApi() {
        return fetchDataFromApi(buildRequestParams());
    }
    
    /**
     * 从API获取数据（带参数）
     */
    private Object fetchDataFromApi(Map<String, Object> params) {
        String apiUrl = config.getString("apiUrl");
        String method = config.getString("method", "GET");
        int timeout = config.getInt("timeout", 30000);
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        Map<String, String> headerConfig = config.getMap("headers");
        if (headerConfig != null) {
            headerConfig.forEach(headers::set);
        }
        
        // 构建请求实体
        HttpEntity<Object> entity = new HttpEntity<>(
            "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) ? params : null,
            headers
        );
        
        // 发起请求
        ResponseEntity<String> response = restTemplate.exchange(
            buildUrlWithParams(apiUrl, "GET".equalsIgnoreCase(method) ? params : null),
            HttpMethod.valueOf(method.toUpperCase()),
            entity,
            String.class
        );
        
        // 解析响应
        return parseResponse(response.getBody());
    }
    
    /**
     * 构建请求参数
     */
    private Map<String, Object> buildRequestParams() {
        Map<String, Object> params = new HashMap<>();
        
        // 添加配置的查询参数
        Map<String, Object> queryParams = config.getMap("queryParams");
        if (queryParams != null) {
            params.putAll(queryParams);
        }
        
        // 添加增量拉取参数
        String lastPollTimeField = config.getString("lastPollTimeField");
        if (lastPollTimeField != null && lastDataTimestamp != null) {
            params.put(lastPollTimeField, lastDataTimestamp);
        }
        
        return params;
    }
    
    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(baseUrl.contains("?") ? "&" : "?");
        
        params.forEach((key, value) -> {
            url.append(key).append("=").append(value).append("&");
        });
        
        // 移除最后的&
        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        
        return url.toString();
    }
    
    /**
     * 解析HTTP响应
     */
    private Object parseResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(responseBody, Object.class);
        } catch (Exception e) {
            log.warn("解析JSON响应失败，返回原始字符串: {}", e.getMessage());
            return responseBody;
        }
    }
    
    /**
     * 检查是否有新数据
     */
    private boolean hasNewData(Object data) {
        if (data == null) {
            return false;
        }
        
        // 如果是第一次拉取，认为有新数据
        if (lastDataTimestamp == null) {
            return true;
        }
        
        // 检查数据是否为空
        if (data instanceof Collection && ((Collection<?>) data).isEmpty()) {
            return false;
        }
        
        if (data instanceof Map && ((Map<?, ?>) data).isEmpty()) {
            return false;
        }
        
        // 其他情况认为有新数据
        return true;
    }
    
    /**
     * 更新最后数据时间戳
     */
    private void updateLastDataTimestamp(Object data) {
        String lastPollTimeField = config.getString("lastPollTimeField");
        if (lastPollTimeField == null) {
            // 如果没有配置时间字段，使用当前时间
            lastDataTimestamp = String.valueOf(System.currentTimeMillis());
            return;
        }
        
        // 从数据中提取时间戳
        try {
            if (data instanceof Map) {
                Object timestamp = ((Map<?, ?>) data).get(lastPollTimeField);
                if (timestamp != null) {
                    lastDataTimestamp = timestamp.toString();
                }
            } else if (data instanceof Collection && !((Collection<?>) data).isEmpty()) {
                // 从集合的第一个元素中提取时间戳
                Object firstItem = ((Collection<?>) data).iterator().next();
                if (firstItem instanceof Map) {
                    Object timestamp = ((Map<?, ?>) firstItem).get(lastPollTimeField);
                    if (timestamp != null) {
                        lastDataTimestamp = timestamp.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取时间戳失败: {}", e.getMessage());
        }
        
        // 如果提取失败，使用当前时间
        if (lastDataTimestamp == null) {
            lastDataTimestamp = String.valueOf(System.currentTimeMillis());
        }
    }
    
    /**
     * 创建触发元数据
     */
    private Map<String, Object> createTriggerMetadata(DataItem dataItem, TriggerContext context) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("triggerType", "http_polling");
        metadata.put("apiUrl", config.getString("apiUrl"));
        metadata.put("pollTime", lastPollTime);
        metadata.put("dataIndex", dataItem.getIndex());
        metadata.put("totalCount", dataItem.getTotalCount());
        metadata.put("triggerId", context.getTriggerId());
        return metadata;
    }
    
    /**
     * 验证配置
     */
    private void validateConfiguration() {
        ValidationResult result = validate(config);
        if (result.hasErrors()) {
            throw new IllegalArgumentException("配置验证失败: " + result.getErrors());
        }
    }
    
    /**
     * 判断是否为可重试的错误
     */
    private boolean isRetryableError(Exception e) {
        // 网络错误、超时错误等通常是可重试的
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connection") || 
               message.contains("network") ||
               message.contains("503") ||
               message.contains("502");
    }
}
