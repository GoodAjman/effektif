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

import com.effektif.dynamic.expression.ExpressionEngine;
import com.effektif.dynamic.node.ActionNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * HTTP 请求动作节点实现
 * 
 * 实现类似 n8n 的 HTTP Request 节点功能，支持发起各种 HTTP 请求，
 * 调用外部 API 接口，并处理响应数据。
 * 
 * 核心功能：
 * 1. 多种HTTP方法：支持GET、POST、PUT、DELETE、PATCH等方法
 * 2. 表达式支持：URL、请求头、请求体都支持表达式解析
 * 3. 数据映射：自动处理请求和响应数据的格式转换
 * 4. 错误处理：完善的错误处理和重试机制
 * 5. 批量请求：支持批量发起多个HTTP请求
 * 
 * 配置参数：
 * - url: 请求URL（支持表达式）
 * - method: HTTP方法（GET、POST、PUT、DELETE、PATCH）
 * - headers: 请求头配置（支持表达式）
 * - body: 请求体内容（支持表达式）
 * - timeout: 请求超时时间（毫秒）
 * - followRedirects: 是否跟随重定向
 * - validateSSL: 是否验证SSL证书
 * - retryCount: 重试次数
 * - retryDelay: 重试延迟（毫秒）
 * 
 * 使用示例：
 * <pre>{@code
 * // 配置HTTP请求动作节点
 * NodeConfig config = NodeConfig.builder()
 *     .put("url", "https://api.example.com/users/{{$json.userId}}")
 *     .put("method", "POST")
 *     .put("headers", Map.of(
 *         "Content-Type", "application/json",
 *         "Authorization", "Bearer {{$workflow.apiToken}}"
 *     ))
 *     .put("body", Map.of(
 *         "name", "{{$json.name}}",
 *         "email", "{{$json.email}}",
 *         "timestamp", "{{= new Date().toISOString()}}"
 *     ))
 *     .put("timeout", 30000)
 *     .put("retryCount", 3)
 *     .build();
 * 
 * // 初始化并执行动作
 * HttpRequestActionNode action = new HttpRequestActionNode();
 * action.initialize(config);
 * 
 * ActionContext context = ActionContext.builder()
 *     .inputData(Map.of("userId", "123", "name", "Alice", "email", "alice@example.com"))
 *     .variable("apiToken", "abc123")
 *     .build();
 * 
 * ActionResult result = action.executeAction(context);
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
@Component
public class HttpRequestActionNode implements ActionNode {
    
    private static final Logger log = LoggerFactory.getLogger(HttpRequestActionNode.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ExpressionEngine expressionEngine;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /** 节点配置 */
    private NodeConfig config;
    
    /** 节点描述符 */
    private static final NodeDescriptor DESCRIPTOR = NodeDescriptor.builder()
            .key("http_request")
            .name("HTTP请求")
            .description("发起HTTP请求调用外部API")
            .category("action")
            .icon("http")
            .inputDescriptor("url", "请求URL", "string", true)
            .inputDescriptor("method", "HTTP方法", "select", false, "GET", 
                           Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"))
            .inputDescriptor("headers", "请求头", "object", false)
            .inputDescriptor("body", "请求体", "object", false)
            .inputDescriptor("timeout", "超时时间(毫秒)", "integer", false, 30000)
            .inputDescriptor("followRedirects", "跟随重定向", "boolean", false, true)
            .inputDescriptor("validateSSL", "验证SSL", "boolean", false, true)
            .inputDescriptor("retryCount", "重试次数", "integer", false, 0)
            .inputDescriptor("retryDelay", "重试延迟(毫秒)", "integer", false, 1000)
            .outputDescriptor("statusCode", "HTTP状态码")
            .outputDescriptor("headers", "响应头")
            .outputDescriptor("body", "响应体")
            .outputDescriptor("responseTime", "响应时间(毫秒)")
            .build();
    
    @Override
    public NodeType getNodeType() {
        return NodeType.ACTION;
    }
    
    @Override
    public ActionType getActionType() {
        return ActionType.HTTP_REQUEST;
    }
    
    @Override
    public NodeDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Override
    public void initialize(NodeConfig config) {
        this.config = config;
        log.info("HTTP请求动作节点初始化完成，URL模板: {}, 方法: {}", 
                config.getString("url"), config.getString("method", "GET"));
    }
    
    @Override
    public boolean supportsBatchOperation() {
        return true;
    }
    
    @Override
    public ExecutionStrategy getExecutionStrategy() {
        return ExecutionStrategy.RETRY;
    }
    
    @Override
    public boolean supportsRetry() {
        return true;
    }
    
    @Override
    public boolean isIdempotent() {
        String method = config.getString("method", "GET");
        // GET、PUT、DELETE通常是幂等的，POST通常不是
        return Arrays.asList("GET", "PUT", "DELETE", "HEAD", "OPTIONS").contains(method.toUpperCase());
    }
    
    @Override
    public int getMaxBatchSize() {
        return config.getInt("maxBatchSize", 50);
    }
    
    @Override
    public long estimateExecutionTime(ActionContext context) {
        int timeout = config.getInt("timeout", 30000);
        // 估算为超时时间的一半
        return timeout / 2;
    }
    
    @Override
    public CompletableFuture<NodeResult> execute(NodeContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ActionContext actionContext = ActionContext.from(context);
                ActionResult result = executeAction(actionContext);
                
                return NodeResult.success(result.getData());
                
            } catch (Exception e) {
                log.error("HTTP请求节点执行失败", e);
                return NodeResult.failure(e);
            }
        });
    }
    
    @Override
    public ActionResult executeAction(ActionContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始执行HTTP请求，上下文: {}", context);
            
            // 1. 解析配置中的表达式
            String url = expressionEngine.evaluate(config.getString("url"), context);
            String method = config.getString("method", "GET").toUpperCase();
            Map<String, String> headers = evaluateHeaders(context);
            Object body = evaluateBody(context);
            
            log.debug("HTTP请求参数 - URL: {}, 方法: {}, 请求头数量: {}", 
                     url, method, headers.size());
            
            // 2. 执行HTTP请求（带重试）
            HttpResponse response = executeHttpRequestWithRetry(url, method, headers, body);
            
            // 3. 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("statusCode", response.getStatusCode());
            responseData.put("headers", response.getHeaders());
            responseData.put("body", response.getBody());
            responseData.put("responseTime", System.currentTimeMillis() - startTime);
            
            log.debug("HTTP请求执行成功，状态码: {}, 响应时间: {}ms", 
                     response.getStatusCode(), responseData.get("responseTime"));
            
            return ActionResult.success(responseData);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            log.error("HTTP请求执行失败，响应时间: {}ms, 错误: {}", responseTime, e.getMessage(), e);
            
            // 构建错误响应数据
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage());
            errorData.put("responseTime", responseTime);
            
            return ActionResult.failure(e, errorData);
        }
    }
    
    @Override
    public List<ActionResult> executeBatchAction(List<ActionContext> contexts) {
        log.info("开始执行批量HTTP请求，数量: {}", contexts.size());
        
        // 并行执行多个HTTP请求
        return contexts.parallelStream()
                .map(this::executeAction)
                .collect(Collectors.toList());
    }
    
    @Override
    public ValidationResult validate(NodeConfig config) {
        ValidationResult.Builder builder = ValidationResult.builder();
        
        // 验证必填参数
        if (config.getString("url") == null || config.getString("url").trim().isEmpty()) {
            builder.addError("url", "请求URL不能为空");
        }
        
        // 验证HTTP方法
        String method = config.getString("method", "GET");
        if (!Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
                .contains(method.toUpperCase())) {
            builder.addError("method", "不支持的HTTP方法: " + method);
        }
        
        // 验证超时时间
        int timeout = config.getInt("timeout", 30000);
        if (timeout < 1000) {
            builder.addWarning("timeout", "超时时间过短可能导致请求失败");
        } else if (timeout > 300000) {
            builder.addWarning("timeout", "超时时间过长可能影响性能");
        }
        
        // 验证重试配置
        int retryCount = config.getInt("retryCount", 0);
        if (retryCount < 0) {
            builder.addError("retryCount", "重试次数不能为负数");
        } else if (retryCount > 10) {
            builder.addWarning("retryCount", "重试次数过多可能影响性能");
        }
        
        int retryDelay = config.getInt("retryDelay", 1000);
        if (retryDelay < 0) {
            builder.addError("retryDelay", "重试延迟不能为负数");
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
    
    /**
     * 评估请求头表达式
     */
    private Map<String, String> evaluateHeaders(ActionContext context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> headerConfig = config.getMap("headers");
        
        if (headerConfig != null) {
            headerConfig.forEach((key, value) -> {
                try {
                    String evaluatedValue = expressionEngine.evaluate(value.toString(), context);
                    headers.put(key, evaluatedValue);
                } catch (Exception e) {
                    log.warn("评估请求头表达式失败，键: {}, 值: {}, 错误: {}", key, value, e.getMessage());
                    headers.put(key, value.toString()); // 使用原始值
                }
            });
        }
        
        // 设置默认请求头
        headers.putIfAbsent("User-Agent", "Effektif-Dynamic-Node/2.0");
        
        return headers;
    }
    
    /**
     * 评估请求体表达式
     */
    private Object evaluateBody(ActionContext context) {
        Object bodyConfig = config.get("body");
        if (bodyConfig == null) {
            return null;
        }
        
        try {
            if (bodyConfig instanceof String) {
                return expressionEngine.evaluate((String) bodyConfig, context);
            } else if (bodyConfig instanceof Map) {
                return evaluateObjectExpressions((Map<String, Object>) bodyConfig, context);
            } else {
                return bodyConfig;
            }
        } catch (Exception e) {
            log.warn("评估请求体表达式失败: {}", e.getMessage());
            return bodyConfig; // 返回原始配置
        }
    }
    
    /**
     * 递归评估对象中的表达式
     */
    @SuppressWarnings("unchecked")
    private Object evaluateObjectExpressions(Map<String, Object> obj, ActionContext context) {
        Map<String, Object> result = new HashMap<>();
        
        obj.forEach((key, value) -> {
            try {
                if (value instanceof String) {
                    result.put(key, expressionEngine.evaluate((String) value, context));
                } else if (value instanceof Map) {
                    result.put(key, evaluateObjectExpressions((Map<String, Object>) value, context));
                } else if (value instanceof List) {
                    List<Object> list = (List<Object>) value;
                    List<Object> evaluatedList = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof String) {
                            evaluatedList.add(expressionEngine.evaluate((String) item, context));
                        } else if (item instanceof Map) {
                            evaluatedList.add(evaluateObjectExpressions((Map<String, Object>) item, context));
                        } else {
                            evaluatedList.add(item);
                        }
                    }
                    result.put(key, evaluatedList);
                } else {
                    result.put(key, value);
                }
            } catch (Exception e) {
                log.warn("评估对象表达式失败，键: {}, 错误: {}", key, e.getMessage());
                result.put(key, value); // 使用原始值
            }
        });
        
        return result;
    }
    
    /**
     * 执行HTTP请求（带重试）
     */
    private HttpResponse executeHttpRequestWithRetry(String url, String method, 
                                                   Map<String, String> headers, Object body) {
        int retryCount = config.getInt("retryCount", 0);
        int retryDelay = config.getInt("retryDelay", 1000);
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    log.debug("HTTP请求重试，第 {} 次尝试", attempt);
                    Thread.sleep(retryDelay);
                }
                
                return executeHttpRequest(url, method, headers, body);
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < retryCount && isRetryableError(e)) {
                    log.warn("HTTP请求失败，将重试，错误: {}", e.getMessage());
                } else {
                    break;
                }
            }
        }
        
        throw new RuntimeException("HTTP请求失败，已重试 " + retryCount + " 次", lastException);
    }
    
    /**
     * 执行HTTP请求
     */
    private HttpResponse executeHttpRequest(String url, String method, 
                                          Map<String, String> headers, Object body) {
        // 构建请求头
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::set);
        
        // 处理请求体
        Object requestBody = null;
        if (body != null && !Arrays.asList("GET", "HEAD", "DELETE").contains(method)) {
            if (body instanceof String) {
                requestBody = body;
                httpHeaders.setContentType(MediaType.TEXT_PLAIN);
            } else {
                try {
                    requestBody = objectMapper.writeValueAsString(body);
                    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                } catch (Exception e) {
                    log.warn("序列化请求体失败，使用toString: {}", e.getMessage());
                    requestBody = body.toString();
                    httpHeaders.setContentType(MediaType.TEXT_PLAIN);
                }
            }
        }
        
        // 构建请求实体
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
        
        // 发起请求
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.valueOf(method),
            entity,
            String.class
        );
        
        // 解析响应
        Object responseBody = parseResponseBody(response.getBody(), response.getHeaders());
        
        return new HttpResponse(
            response.getStatusCodeValue(),
            convertHeaders(response.getHeaders()),
            responseBody
        );
    }
    
    /**
     * 解析响应体
     */
    private Object parseResponseBody(String responseBody, HttpHeaders headers) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return null;
        }
        
        // 根据Content-Type决定如何解析
        MediaType contentType = headers.getContentType();
        if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
            try {
                return objectMapper.readValue(responseBody, Object.class);
            } catch (Exception e) {
                log.debug("JSON解析失败，返回原始字符串: {}", e.getMessage());
                return responseBody;
            }
        } else {
            return responseBody;
        }
    }
    
    /**
     * 转换响应头格式
     */
    private Map<String, String> convertHeaders(HttpHeaders headers) {
        Map<String, String> result = new HashMap<>();
        headers.forEach((key, values) -> {
            if (!values.isEmpty()) {
                result.put(key, String.join(", ", values));
            }
        });
        return result;
    }
    
    /**
     * 判断是否为可重试的错误
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connection") || 
               message.contains("network") ||
               message.contains("503") ||
               message.contains("502") ||
               message.contains("504");
    }
    
    /**
     * HTTP响应封装类
     */
    private static class HttpResponse {
        private final int statusCode;
        private final Map<String, String> headers;
        private final Object body;
        
        public HttpResponse(int statusCode, Map<String, String> headers, Object body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }
        
        public int getStatusCode() { return statusCode; }
        public Map<String, String> getHeaders() { return headers; }
        public Object getBody() { return body; }
    }
}
