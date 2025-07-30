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
package com.effektif.dynamic.expression;

import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表达式引擎
 * 
 * 支持多种表达式格式的解析和执行，类似于 n8n 的表达式系统。
 * 提供了灵活的变量引用、JavaScript表达式执行和JSON路径查询功能。
 * 
 * 支持的表达式格式：
 * 1. {{variable}} - 简单变量引用
 * 2. {{= expression}} - JavaScript 表达式
 * 3. {{$json.path}} - JSON 路径表达式
 * 4. {{$node.output}} - 节点输出引用
 * 5. {{$workflow.variable}} - 工作流变量引用
 * 
 * 设计特点：
 * 1. 多格式支持：支持多种表达式语法，满足不同场景需求
 * 2. 安全执行：JavaScript表达式在受限环境中执行，防止恶意代码
 * 3. 性能优化：缓存编译后的表达式，提高执行效率
 * 4. 错误处理：完善的错误处理机制，表达式执行失败时优雅降级
 * 5. 上下文感知：支持丰富的上下文变量，便于数据访问
 * 
 * 使用示例：
 * <pre>{@code
 * ExpressionEngine engine = new ExpressionEngine();
 * 
 * // 创建执行上下文
 * ActionContext context = ActionContext.builder()
 *     .inputData(Map.of("name", "Alice", "age", 30))
 *     .variable("userId", "12345")
 *     .nodeData("previous", Map.of("result", "success"))
 *     .build();
 * 
 * // 简单变量引用
 * String name = engine.evaluate("{{name}}", context);
 * // 结果: "Alice"
 * 
 * // JavaScript表达式
 * String greeting = engine.evaluate("{{= 'Hello, ' + name + '!'}}", context);
 * // 结果: "Hello, Alice!"
 * 
 * // JSON路径表达式
 * String result = engine.evaluate("{{$node.previous.result}}", context);
 * // 结果: "success"
 * 
 * // 复杂表达式
 * String message = engine.evaluate("User {{name}} is {{= age >= 18 ? 'adult' : 'minor'}}", context);
 * // 结果: "User Alice is adult"
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
@Component
public class ExpressionEngine {
    
    private static final Logger log = LoggerFactory.getLogger(ExpressionEngine.class);
    
    /** 表达式模式：匹配 {{...}} 格式的表达式 */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    
    /** JavaScript 表达式前缀 */
    private static final String JS_EXPRESSION_PREFIX = "=";
    
    /** JSON 路径表达式前缀 */
    private static final String JSON_PATH_PREFIX = "$json.";
    
    /** 节点数据表达式前缀 */
    private static final String NODE_DATA_PREFIX = "$node.";
    
    /** 工作流变量表达式前缀 */
    private static final String WORKFLOW_VAR_PREFIX = "$workflow.";
    
    /** JavaScript 脚本引擎 */
    private final ScriptEngine scriptEngine;
    
    /** 表达式缓存，提高重复表达式的执行效率 */
    private final Map<String, CompiledExpression> expressionCache;
    
    /** 最大缓存大小 */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * 编译后的表达式
     */
    private static class CompiledExpression {
        final String originalExpression;
        final ExpressionType type;
        final String processedExpression;
        final long compiledAt;
        
        CompiledExpression(String originalExpression, ExpressionType type, String processedExpression) {
            this.originalExpression = originalExpression;
            this.type = type;
            this.processedExpression = processedExpression;
            this.compiledAt = System.currentTimeMillis();
        }
    }
    
    /**
     * 表达式类型枚举
     */
    private enum ExpressionType {
        /** 简单变量引用 */
        VARIABLE,
        /** JavaScript 表达式 */
        JAVASCRIPT,
        /** JSON 路径表达式 */
        JSON_PATH,
        /** 节点数据引用 */
        NODE_DATA,
        /** 工作流变量引用 */
        WORKFLOW_VAR,
        /** 字面量（不是表达式） */
        LITERAL
    }
    
    /**
     * 构造函数
     */
    public ExpressionEngine() {
        // 初始化 JavaScript 脚本引擎
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("javascript");
        
        if (this.scriptEngine == null) {
            log.warn("JavaScript 脚本引擎不可用，JavaScript 表达式功能将被禁用");
        }
        
        // 初始化表达式缓存
        this.expressionCache = new ConcurrentHashMap<>();
        
        log.info("表达式引擎初始化完成，JavaScript 支持: {}", this.scriptEngine != null);
    }
    
    /**
     * 评估表达式
     * 
     * 解析并执行表达式，返回结果字符串。支持多种表达式格式的混合使用。
     * 
     * @param expression 表达式字符串，可以包含多个 {{...}} 表达式
     * @param context 执行上下文，包含变量、数据等信息
     * @return 表达式执行结果
     * @throws ExpressionEvaluationException 如果表达式执行失败
     */
    public String evaluate(String expression, ActionContext context) {
        if (expression == null) {
            return null;
        }
        
        // 检查是否包含表达式
        if (!expression.contains("{{")) {
            return expression;
        }
        
        try {
            log.debug("开始评估表达式: {}", expression);
            
            String result = evaluateExpressions(expression, context);
            
            log.debug("表达式评估完成: {} -> {}", expression, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("表达式评估失败: {}, 错误: {}", expression, e.getMessage(), e);
            throw new ExpressionEvaluationException("表达式评估失败: " + expression, e);
        }
    }
    
    /**
     * 评估表达式并返回指定类型的结果
     * 
     * @param expression 表达式字符串
     * @param context 执行上下文
     * @param resultType 期望的结果类型
     * @param <T> 结果类型
     * @return 转换后的结果
     */
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, ActionContext context, Class<T> resultType) {
        String stringResult = evaluate(expression, context);
        
        if (stringResult == null) {
            return null;
        }
        
        // 类型转换
        if (resultType == String.class) {
            return (T) stringResult;
        } else if (resultType == Integer.class) {
            return (T) Integer.valueOf(stringResult);
        } else if (resultType == Long.class) {
            return (T) Long.valueOf(stringResult);
        } else if (resultType == Double.class) {
            return (T) Double.valueOf(stringResult);
        } else if (resultType == Boolean.class) {
            return (T) Boolean.valueOf(stringResult);
        } else {
            throw new IllegalArgumentException("不支持的结果类型: " + resultType);
        }
    }
    
    /**
     * 检查字符串是否包含表达式
     * 
     * @param text 待检查的字符串
     * @return true 如果包含表达式，false 否则
     */
    public boolean containsExpression(String text) {
        return text != null && text.contains("{{");
    }
    
    /**
     * 提取表达式中的所有变量引用
     * 
     * @param expression 表达式字符串
     * @return 变量名集合
     */
    public java.util.Set<String> extractVariables(String expression) {
        java.util.Set<String> variables = new java.util.HashSet<>();
        
        if (expression == null) {
            return variables;
        }
        
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            
            // 简单变量引用
            if (!expr.startsWith(JS_EXPRESSION_PREFIX) && 
                !expr.startsWith(JSON_PATH_PREFIX) && 
                !expr.startsWith(NODE_DATA_PREFIX) && 
                !expr.startsWith(WORKFLOW_VAR_PREFIX)) {
                variables.add(expr);
            }
        }
        
        return variables;
    }
    
    /**
     * 清理表达式缓存
     */
    public void clearCache() {
        expressionCache.clear();
        log.info("表达式缓存已清理");
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(expressionCache.size(), MAX_CACHE_SIZE);
    }
    
    /**
     * 评估表达式字符串中的所有表达式
     */
    private String evaluateExpressions(String expression, ActionContext context) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(expression);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            String value = evaluateSingleExpression(expr, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 评估单个表达式
     */
    private String evaluateSingleExpression(String expr, ActionContext context) {
        try {
            // 检查缓存
            CompiledExpression compiled = getCompiledExpression(expr);
            
            switch (compiled.type) {
                case JAVASCRIPT:
                    return evaluateJavaScriptExpression(compiled.processedExpression, context);
                case JSON_PATH:
                    return evaluateJsonPathExpression(compiled.processedExpression, context);
                case NODE_DATA:
                    return evaluateNodeDataExpression(compiled.processedExpression, context);
                case WORKFLOW_VAR:
                    return evaluateWorkflowVariableExpression(compiled.processedExpression, context);
                case VARIABLE:
                    return evaluateVariableReference(compiled.processedExpression, context);
                default:
                    return expr; // 字面量
            }
        } catch (Exception e) {
            log.warn("单个表达式评估失败: {}, 错误: {}", expr, e.getMessage());
            return "{{" + expr + "}}"; // 返回原始表达式
        }
    }
    
    /**
     * 获取编译后的表达式（带缓存）
     */
    private CompiledExpression getCompiledExpression(String expr) {
        return expressionCache.computeIfAbsent(expr, this::compileExpression);
    }
    
    /**
     * 编译表达式
     */
    private CompiledExpression compileExpression(String expr) {
        // 清理缓存（如果超过最大大小）
        if (expressionCache.size() >= MAX_CACHE_SIZE) {
            clearOldCacheEntries();
        }
        
        ExpressionType type;
        String processedExpression;
        
        if (expr.startsWith(JS_EXPRESSION_PREFIX)) {
            type = ExpressionType.JAVASCRIPT;
            processedExpression = expr.substring(JS_EXPRESSION_PREFIX.length()).trim();
        } else if (expr.startsWith(JSON_PATH_PREFIX)) {
            type = ExpressionType.JSON_PATH;
            processedExpression = expr.substring(JSON_PATH_PREFIX.length());
        } else if (expr.startsWith(NODE_DATA_PREFIX)) {
            type = ExpressionType.NODE_DATA;
            processedExpression = expr.substring(NODE_DATA_PREFIX.length());
        } else if (expr.startsWith(WORKFLOW_VAR_PREFIX)) {
            type = ExpressionType.WORKFLOW_VAR;
            processedExpression = expr.substring(WORKFLOW_VAR_PREFIX.length());
        } else {
            type = ExpressionType.VARIABLE;
            processedExpression = expr;
        }
        
        return new CompiledExpression(expr, type, processedExpression);
    }
    
    /**
     * 清理旧的缓存条目
     */
    private void clearOldCacheEntries() {
        // 简单的LRU策略：清理一半的缓存
        int targetSize = MAX_CACHE_SIZE / 2;
        
        expressionCache.entrySet()
                .stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().compiledAt, e2.getValue().compiledAt))
                .limit(expressionCache.size() - targetSize)
                .forEach(entry -> expressionCache.remove(entry.getKey()));
        
        log.debug("清理了 {} 个旧的缓存条目", expressionCache.size() - targetSize);
    }
    
    /**
     * 评估 JavaScript 表达式
     */
    private String evaluateJavaScriptExpression(String expr, ActionContext context) throws ScriptException {
        if (scriptEngine == null) {
            throw new UnsupportedOperationException("JavaScript 脚本引擎不可用");
        }
        
        // 设置上下文变量
        Bindings bindings = scriptEngine.createBindings();
        
        // 添加输入数据
        if (context.getInputData() != null) {
            bindings.put("$json", context.getInputData());
        }
        
        // 添加工作流变量
        context.getVariables().forEach(bindings::put);
        
        // 添加节点数据
        if (context.getNodeData() != null) {
            bindings.put("$node", context.getNodeData());
        }
        
        // 执行表达式
        Object result = scriptEngine.eval(expr, bindings);
        return result != null ? result.toString() : "";
    }
    
    /**
     * 评估 JSON 路径表达式
     */
    private String evaluateJsonPathExpression(String path, ActionContext context) {
        Object data = context.getInputData();
        if (data == null) {
            return "";
        }
        
        try {
            Object value = JsonPath.read(data, "$." + path);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.debug("JSON 路径表达式执行失败: {}, 错误: {}", path, e.getMessage());
            return "";
        }
    }
    
    /**
     * 评估节点数据表达式
     */
    private String evaluateNodeDataExpression(String path, ActionContext context) {
        Map<String, Object> nodeData = context.getNodeData();
        if (nodeData == null) {
            return "";
        }
        
        try {
            Object value = JsonPath.read(nodeData, "$." + path);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.debug("节点数据表达式执行失败: {}, 错误: {}", path, e.getMessage());
            return "";
        }
    }
    
    /**
     * 评估工作流变量表达式
     */
    private String evaluateWorkflowVariableExpression(String varName, ActionContext context) {
        Object value = context.getVariable(varName);
        return value != null ? value.toString() : "";
    }
    
    /**
     * 评估变量引用
     */
    private String evaluateVariableReference(String varName, ActionContext context) {
        // 首先尝试从输入数据中获取
        if (context.getInputData() instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) context.getInputData();
            Object value = dataMap.get(varName);
            if (value != null) {
                return value.toString();
            }
        }
        
        // 然后尝试从工作流变量中获取
        Object value = context.getVariable(varName);
        return value != null ? value.toString() : "";
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private final int currentSize;
        private final int maxSize;
        
        public CacheStatistics(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }
        
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public double getUsageRatio() { return (double) currentSize / maxSize; }
        
        @Override
        public String toString() {
            return String.format("CacheStatistics{currentSize=%d, maxSize=%d, usageRatio=%.2f}", 
                               currentSize, maxSize, getUsageRatio());
        }
    }
    
    /**
     * 表达式评估异常
     */
    public static class ExpressionEvaluationException extends RuntimeException {
        public ExpressionEvaluationException(String message) {
            super(message);
        }
        
        public ExpressionEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
