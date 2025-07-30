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
package com.effektif.dynamic.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 数据拆分器
 * 
 * 实现类似 Zapier 的数据自动拆分功能，将批量数据拆分为单条数据项，
 * 以便工作流能够独立处理每条数据。这是实现数据驱动工作流的核心组件。
 * 
 * 设计理念：
 * 1. 自动检测：智能检测数据格式，自动选择合适的拆分策略
 * 2. 多格式支持：支持JSON数组、List、Map等多种数据格式
 * 3. 自定义路径：支持使用JSONPath表达式指定拆分路径
 * 4. 元数据保留：为每个拆分的数据项保留索引、总数等元数据
 * 5. 错误容忍：拆分失败时优雅降级，不中断整个流程
 * 
 * 拆分策略：
 * - AUTO_DETECT: 自动检测数据类型并选择最佳拆分方式
 * - FORCE_ARRAY: 强制将数据作为数组处理
 * - SINGLE_ITEM: 将数据作为单个项目处理，不进行拆分
 * - CUSTOM_PATH: 使用自定义JSONPath表达式拆分数据
 * 
 * 使用示例：
 * <pre>{@code
 * DataSplitter splitter = new DataSplitter();
 * 
 * // 自动拆分JSON数组
 * String jsonData = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
 * List<DataItem> items = splitter.splitData(jsonData, SplitStrategy.AUTO_DETECT, null);
 * // 结果：2个DataItem，分别包含Alice和Bob的数据
 * 
 * // 使用自定义路径拆分
 * String complexData = "{\"users\":[{\"id\":1},{\"id\":2}],\"total\":2}";
 * List<DataItem> users = splitter.splitData(complexData, SplitStrategy.CUSTOM_PATH, "$.users");
 * // 结果：2个DataItem，从users数组中拆分
 * 
 * // 处理单个对象
 * Map<String, Object> singleUser = Map.of("id", 1, "name", "Alice");
 * List<DataItem> single = splitter.splitData(singleUser, SplitStrategy.AUTO_DETECT, null);
 * // 结果：1个DataItem，包含单个用户数据
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
@Component
public class DataSplitter {
    
    private static final Logger log = LoggerFactory.getLogger(DataSplitter.class);
    
    private final ObjectMapper objectMapper;
    
    /**
     * 拆分策略枚举
     * 
     * 定义了数据拆分的不同策略，每种策略适用于不同的数据格式和场景：
     */
    public enum SplitStrategy {
        /** 自动检测数据类型并选择最佳拆分方式 */
        AUTO_DETECT("auto", "自动检测", 
                   "智能检测数据格式，自动选择最合适的拆分策略"),
        
        /** 强制将数据作为数组处理，即使是单个对象也包装为数组 */
        FORCE_ARRAY("force_array", "强制数组", 
                   "强制将数据作为数组处理，单个对象也会包装为数组"),
        
        /** 将数据作为单个项目处理，不进行任何拆分 */
        SINGLE_ITEM("single", "单项处理", 
                   "将数据作为单个项目处理，不进行拆分"),
        
        /** 使用自定义JSONPath表达式拆分数据 */
        CUSTOM_PATH("custom_path", "自定义路径", 
                   "使用JSONPath表达式指定数据拆分的路径");
        
        private final String code;
        private final String name;
        private final String description;
        
        SplitStrategy(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    /**
     * 构造函数
     * 
     * @param objectMapper Jackson ObjectMapper实例，用于JSON处理
     */
    public DataSplitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 默认构造函数
     * 使用默认的ObjectMapper配置
     */
    public DataSplitter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 拆分数据
     * 
     * 根据指定的策略拆分数据，返回数据项列表。每个数据项包含：
     * - 实际数据内容
     * - 在原始数据中的索引位置
     * - 总数据项数量
     * - 其他元数据信息
     * 
     * @param data 原始数据，可以是任意类型的对象
     * @param strategy 拆分策略
     * @param customPath 自定义路径（仅在strategy为CUSTOM_PATH时使用）
     * @return 拆分后的数据项列表
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果拆分过程中发生错误
     */
    public List<DataItem> splitData(Object data, SplitStrategy strategy, String customPath) {
        if (data == null) {
            log.debug("输入数据为null，返回空列表");
            return Collections.emptyList();
        }
        
        if (strategy == null) {
            log.debug("拆分策略为null，使用AUTO_DETECT策略");
            strategy = SplitStrategy.AUTO_DETECT;
        }
        
        try {
            log.debug("开始拆分数据，策略: {}, 数据类型: {}", strategy, data.getClass().getSimpleName());
            
            List<DataItem> result;
            switch (strategy) {
                case AUTO_DETECT:
                    result = autoDetectAndSplit(data);
                    break;
                case FORCE_ARRAY:
                    result = forceArraySplit(data);
                    break;
                case SINGLE_ITEM:
                    result = singleItemSplit(data);
                    break;
                case CUSTOM_PATH:
                    if (customPath == null || customPath.trim().isEmpty()) {
                        throw new IllegalArgumentException("使用CUSTOM_PATH策略时必须提供customPath参数");
                    }
                    result = customPathSplit(data, customPath);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的拆分策略: " + strategy);
            }
            
            log.debug("数据拆分完成，原始数据类型: {}, 拆分后数量: {}", 
                     data.getClass().getSimpleName(), result.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("数据拆分失败，策略: {}, 错误: {}", strategy, e.getMessage(), e);
            // 拆分失败时，将原始数据作为单个项目返回，确保流程不中断
            return singleItemSplit(data);
        }
    }
    
    /**
     * 自动检测数据类型并拆分
     * 
     * 智能检测数据的类型和结构，选择最合适的拆分方式：
     * 1. 如果是List类型，直接拆分列表元素
     * 2. 如果是JSON字符串，尝试解析并拆分
     * 3. 如果是Map类型，检查是否包含数组字段
     * 4. 其他类型作为单个项目处理
     * 
     * @param data 原始数据
     * @return 拆分后的数据项列表
     */
    private List<DataItem> autoDetectAndSplit(Object data) {
        if (data instanceof List) {
            log.debug("检测到List类型，直接拆分列表元素");
            return splitList((List<?>) data);
        } else if (data instanceof String) {
            log.debug("检测到String类型，尝试解析为JSON");
            return splitJsonString((String) data);
        } else if (data instanceof Map) {
            log.debug("检测到Map类型，检查是否包含数组字段");
            return splitMap((Map<?, ?>) data);
        } else if (data.getClass().isArray()) {
            log.debug("检测到数组类型，转换为List后拆分");
            return splitArray(data);
        } else {
            log.debug("检测到其他类型 {}，作为单个项目处理", data.getClass().getSimpleName());
            return singleItemSplit(data);
        }
    }
    
    /**
     * 强制数组拆分
     * 
     * 无论输入数据是什么类型，都尝试将其作为数组处理。
     * 如果不是数组类型，则将其包装为单元素数组。
     * 
     * @param data 原始数据
     * @return 拆分后的数据项列表
     */
    private List<DataItem> forceArraySplit(Object data) {
        if (data instanceof List) {
            return splitList((List<?>) data);
        } else if (data instanceof String) {
            // 尝试解析为JSON数组
            try {
                JsonNode jsonNode = objectMapper.readTree((String) data);
                if (jsonNode.isArray()) {
                    return splitJsonArray(jsonNode);
                } else {
                    // 不是数组，包装为单元素数组
                    return Collections.singletonList(new DataItem(data, 0, 1));
                }
            } catch (Exception e) {
                log.debug("JSON解析失败，将字符串作为单个元素处理");
                return Collections.singletonList(new DataItem(data, 0, 1));
            }
        } else if (data.getClass().isArray()) {
            return splitArray(data);
        } else {
            // 其他类型包装为单元素数组
            return Collections.singletonList(new DataItem(data, 0, 1));
        }
    }
    
    /**
     * 单项处理
     * 
     * 将数据作为单个项目处理，不进行任何拆分。
     * 
     * @param data 原始数据
     * @return 包含单个数据项的列表
     */
    private List<DataItem> singleItemSplit(Object data) {
        return Collections.singletonList(new DataItem(data, 0, 1));
    }
    
    /**
     * 自定义路径拆分
     * 
     * 使用JSONPath表达式从数据中提取指定路径的数组，然后拆分。
     * 
     * @param data 原始数据
     * @param jsonPath JSONPath表达式
     * @return 拆分后的数据项列表
     */
    private List<DataItem> customPathSplit(Object data, String jsonPath) {
        try {
            // 将数据转换为JSON字符串（如果不是的话）
            String jsonString;
            if (data instanceof String) {
                jsonString = (String) data;
            } else {
                jsonString = objectMapper.writeValueAsString(data);
            }
            
            // 使用JSONPath提取数据
            Object extractedData = JsonPath.read(jsonString, jsonPath);
            
            // 递归拆分提取的数据
            return autoDetectAndSplit(extractedData);
            
        } catch (Exception e) {
            log.warn("使用JSONPath {} 拆分数据失败: {}", jsonPath, e.getMessage());
            // 失败时返回原始数据作为单个项目
            return singleItemSplit(data);
        }
    }
    
    /**
     * 拆分List数据
     */
    private List<DataItem> splitList(List<?> list) {
        List<DataItem> result = new ArrayList<>();
        int total = list.size();
        
        for (int i = 0; i < total; i++) {
            Object item = list.get(i);
            DataItem dataItem = new DataItem(item, i, total);
            result.add(dataItem);
        }
        
        return result;
    }
    
    /**
     * 拆分JSON字符串
     */
    private List<DataItem> splitJsonString(String jsonString) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            
            if (jsonNode.isArray()) {
                return splitJsonArray(jsonNode);
            } else if (jsonNode.isObject()) {
                // 检查对象是否包含数组字段
                return checkObjectForArrays(jsonNode);
            } else {
                // 基本类型，作为单个项目
                Object value = objectMapper.treeToValue(jsonNode, Object.class);
                return Collections.singletonList(new DataItem(value, 0, 1));
            }
        } catch (Exception e) {
            log.debug("JSON解析失败，将字符串作为单个项目处理: {}", e.getMessage());
            return Collections.singletonList(new DataItem(jsonString, 0, 1));
        }
    }
    
    /**
     * 拆分JSON数组
     */
    private List<DataItem> splitJsonArray(JsonNode arrayNode) {
        List<DataItem> result = new ArrayList<>();
        int total = arrayNode.size();
        
        for (int i = 0; i < total; i++) {
            JsonNode item = arrayNode.get(i);
            try {
                Object value = objectMapper.treeToValue(item, Object.class);
                DataItem dataItem = new DataItem(value, i, total);
                result.add(dataItem);
            } catch (Exception e) {
                log.warn("转换JSON数组元素失败，索引: {}, 错误: {}", i, e.getMessage());
                // 转换失败时，使用JsonNode本身
                DataItem dataItem = new DataItem(item, i, total);
                result.add(dataItem);
            }
        }
        
        return result;
    }
    
    /**
     * 拆分Map数据
     */
    private List<DataItem> splitMap(Map<?, ?> map) {
        // 检查Map中是否有明显的数组字段
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List && !((List<?>) value).isEmpty()) {
                // 找到第一个非空List，拆分它
                log.debug("在Map中找到List字段: {}, 大小: {}", entry.getKey(), ((List<?>) value).size());
                return splitList((List<?>) value);
            }
        }
        
        // 没有找到数组字段，将整个Map作为单个项目
        return Collections.singletonList(new DataItem(map, 0, 1));
    }
    
    /**
     * 拆分数组数据
     */
    private List<DataItem> splitArray(Object array) {
        List<Object> list = new ArrayList<>();
        
        if (array instanceof Object[]) {
            Collections.addAll(list, (Object[]) array);
        } else {
            // 处理基本类型数组
            int length = java.lang.reflect.Array.getLength(array);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(array, i));
            }
        }
        
        return splitList(list);
    }
    
    /**
     * 检查JSON对象是否包含数组字段
     */
    private List<DataItem> checkObjectForArrays(JsonNode objectNode) {
        // 查找第一个数组字段
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            
            if (value.isArray() && value.size() > 0) {
                log.debug("在JSON对象中找到数组字段: {}, 大小: {}", field.getKey(), value.size());
                return splitJsonArray(value);
            }
        }
        
        // 没有找到数组字段，将整个对象作为单个项目
        try {
            Object obj = objectMapper.treeToValue(objectNode, Object.class);
            return Collections.singletonList(new DataItem(obj, 0, 1));
        } catch (Exception e) {
            log.warn("转换JSON对象失败: {}", e.getMessage());
            return Collections.singletonList(new DataItem(objectNode, 0, 1));
        }
    }
    
    /**
     * 验证数据是否为数组格式
     * 
     * @param data 待验证的数据
     * @return true 如果是数组格式，false 否则
     */
    public boolean isArrayData(Object data) {
        if (data instanceof List) {
            return true;
        }
        
        if (data instanceof String) {
            try {
                JsonNode jsonNode = objectMapper.readTree((String) data);
                return jsonNode.isArray();
            } catch (Exception e) {
                return false;
            }
        }
        
        return data != null && data.getClass().isArray();
    }
    
    /**
     * 获取数组大小
     * 
     * @param data 数据对象
     * @return 数组大小，如果不是数组则返回1
     */
    public int getArraySize(Object data) {
        List<DataItem> items = splitData(data, SplitStrategy.AUTO_DETECT, null);
        return items.size();
    }
}
