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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数据项类
 * 
 * 表示经过数据拆分器处理后的单个数据项。每个数据项包含：
 * 1. 实际的数据内容
 * 2. 在原始数据中的位置信息（索引、总数）
 * 3. 元数据信息（创建时间、数据类型等）
 * 4. 处理状态信息
 * 
 * 这个类是数据驱动工作流的基础单元，每个DataItem通常对应
 * 一个独立的工作流实例执行。
 * 
 * 设计特点：
 * 1. 不可变性：核心数据字段设计为不可变，确保线程安全
 * 2. 元数据丰富：提供完整的元数据信息，便于调试和监控
 * 3. 类型安全：提供类型安全的数据访问方法
 * 4. 序列化支持：支持JSON序列化，便于存储和传输
 * 
 * 使用示例：
 * <pre>{@code
 * // 创建数据项
 * Map<String, Object> userData = Map.of("id", 1, "name", "Alice");
 * DataItem item = new DataItem(userData, 0, 5);
 * 
 * // 访问数据
 * Object data = item.getData();
 * int index = item.getIndex();
 * int total = item.getTotalCount();
 * 
 * // 类型安全访问
 * Map<String, Object> userMap = item.getDataAsMap();
 * String userName = (String) userMap.get("name");
 * 
 * // 检查数据类型
 * if (item.isMapData()) {
 *     // 处理Map类型数据
 * } else if (item.isListData()) {
 *     // 处理List类型数据
 * }
 * 
 * // 添加元数据
 * item.addMetadata("source", "api");
 * item.addMetadata("processed", true);
 * }</pre>
 * 
 * @author Dynamic Node Team
 * @version 2.0
 * @since 1.0
 */
public class DataItem {
    
    /** 实际的数据内容 */
    private final Object data;
    
    /** 在原始数据中的索引位置（从0开始） */
    private final int index;
    
    /** 原始数据拆分后的总数据项数量 */
    private final int totalCount;
    
    /** 数据项创建时间 */
    private final LocalDateTime createdAt;
    
    /** 数据类型的字符串表示 */
    private final String dataType;
    
    /** 数据大小（字节数，用于监控） */
    private final long dataSize;
    
    /** 元数据映射，用于存储额外信息 */
    private final Map<String, Object> metadata;
    
    /** 处理状态 */
    private volatile ProcessingStatus status;
    
    /** 错误信息（如果处理失败） */
    private volatile String errorMessage;
    
    /** 处理开始时间 */
    private volatile LocalDateTime processingStartTime;
    
    /** 处理结束时间 */
    private volatile LocalDateTime processingEndTime;
    
    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        /** 待处理 */
        PENDING("pending", "待处理"),
        
        /** 处理中 */
        PROCESSING("processing", "处理中"),
        
        /** 处理成功 */
        SUCCESS("success", "处理成功"),
        
        /** 处理失败 */
        FAILED("failed", "处理失败"),
        
        /** 已跳过 */
        SKIPPED("skipped", "已跳过");
        
        private final String code;
        private final String name;
        
        ProcessingStatus(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
    }
    
    /**
     * 构造函数
     * 
     * @param data 实际的数据内容
     * @param index 在原始数据中的索引位置
     * @param totalCount 原始数据拆分后的总数据项数量
     */
    public DataItem(Object data, int index, int totalCount) {
        this.data = data;
        this.index = index;
        this.totalCount = totalCount;
        this.createdAt = LocalDateTime.now();
        this.dataType = data != null ? data.getClass().getSimpleName() : "null";
        this.dataSize = estimateDataSize(data);
        this.metadata = new HashMap<>();
        this.status = ProcessingStatus.PENDING;
        
        // 添加基础元数据
        this.metadata.put("createdAt", createdAt);
        this.metadata.put("dataType", dataType);
        this.metadata.put("dataSize", dataSize);
    }
    
    /**
     * 获取数据内容
     * 
     * @return 实际的数据内容
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 获取数据内容（类型安全）
     * 
     * @param clazz 期望的数据类型
     * @param <T> 泛型类型
     * @return 转换后的数据，如果类型不匹配则返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data != null && clazz.isAssignableFrom(data.getClass())) {
            return (T) data;
        }
        return null;
    }
    
    /**
     * 获取数据内容作为Map
     * 
     * @return Map类型的数据，如果不是Map类型则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDataAsMap() {
        return getDataAs(Map.class);
    }
    
    /**
     * 获取数据内容作为List
     * 
     * @return List类型的数据，如果不是List类型则返回null
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Object> getDataAsList() {
        return getDataAs(java.util.List.class);
    }
    
    /**
     * 获取数据内容作为String
     * 
     * @return String类型的数据，如果不是String类型则返回toString()结果
     */
    public String getDataAsString() {
        return data != null ? data.toString() : null;
    }
    
    /**
     * 检查数据是否为Map类型
     * 
     * @return true 如果数据是Map类型，false 否则
     */
    public boolean isMapData() {
        return data instanceof Map;
    }
    
    /**
     * 检查数据是否为List类型
     * 
     * @return true 如果数据是List类型，false 否则
     */
    public boolean isListData() {
        return data instanceof java.util.List;
    }
    
    /**
     * 检查数据是否为String类型
     * 
     * @return true 如果数据是String类型，false 否则
     */
    public boolean isStringData() {
        return data instanceof String;
    }
    
    /**
     * 检查数据是否为null
     * 
     * @return true 如果数据为null，false 否则
     */
    public boolean isNullData() {
        return data == null;
    }
    
    /**
     * 获取索引位置
     * 
     * @return 在原始数据中的索引位置（从0开始）
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * 获取总数据项数量
     * 
     * @return 原始数据拆分后的总数据项数量
     */
    public int getTotalCount() {
        return totalCount;
    }
    
    /**
     * 检查是否为第一个数据项
     * 
     * @return true 如果是第一个数据项，false 否则
     */
    public boolean isFirst() {
        return index == 0;
    }
    
    /**
     * 检查是否为最后一个数据项
     * 
     * @return true 如果是最后一个数据项，false 否则
     */
    public boolean isLast() {
        return index == totalCount - 1;
    }
    
    /**
     * 获取创建时间
     * 
     * @return 数据项创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 获取数据类型
     * 
     * @return 数据类型的字符串表示
     */
    public String getDataType() {
        return dataType;
    }
    
    /**
     * 获取数据大小
     * 
     * @return 数据大小（字节数）
     */
    public long getDataSize() {
        return dataSize;
    }
    
    /**
     * 获取处理状态
     * 
     * @return 当前处理状态
     */
    public ProcessingStatus getStatus() {
        return status;
    }
    
    /**
     * 设置处理状态
     * 
     * @param status 新的处理状态
     */
    public void setStatus(ProcessingStatus status) {
        this.status = status;
        
        // 自动设置时间戳
        if (status == ProcessingStatus.PROCESSING) {
            this.processingStartTime = LocalDateTime.now();
        } else if (status == ProcessingStatus.SUCCESS || status == ProcessingStatus.FAILED) {
            this.processingEndTime = LocalDateTime.now();
        }
    }
    
    /**
     * 获取错误信息
     * 
     * @return 错误信息，如果没有错误则返回null
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 设置错误信息
     * 
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            setStatus(ProcessingStatus.FAILED);
        }
    }
    
    /**
     * 获取处理开始时间
     * 
     * @return 处理开始时间，如果尚未开始处理则返回null
     */
    public LocalDateTime getProcessingStartTime() {
        return processingStartTime;
    }
    
    /**
     * 获取处理结束时间
     * 
     * @return 处理结束时间，如果尚未结束处理则返回null
     */
    public LocalDateTime getProcessingEndTime() {
        return processingEndTime;
    }
    
    /**
     * 获取处理耗时
     * 
     * @return 处理耗时（毫秒），如果尚未完成处理则返回-1
     */
    public long getProcessingDuration() {
        if (processingStartTime != null && processingEndTime != null) {
            return java.time.Duration.between(processingStartTime, processingEndTime).toMillis();
        }
        return -1;
    }
    
    /**
     * 添加元数据
     * 
     * @param key 元数据键
     * @param value 元数据值
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据
     * 
     * @param key 元数据键
     * @return 元数据值，如果不存在则返回null
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * 获取所有元数据
     * 
     * @return 元数据映射的副本
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * 检查是否包含指定的元数据
     * 
     * @param key 元数据键
     * @return true 如果包含该元数据，false 否则
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * 估算数据大小
     * 
     * @param data 数据对象
     * @return 估算的数据大小（字节数）
     */
    private long estimateDataSize(Object data) {
        if (data == null) {
            return 0;
        }
        
        if (data instanceof String) {
            return ((String) data).length() * 2; // 假设UTF-16编码
        } else if (data instanceof Map) {
            return ((Map<?, ?>) data).size() * 50; // 粗略估算
        } else if (data instanceof java.util.List) {
            return ((java.util.List<?>) data).size() * 30; // 粗略估算
        } else {
            return data.toString().length() * 2; // 使用toString长度估算
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataItem dataItem = (DataItem) o;
        return index == dataItem.index &&
               totalCount == dataItem.totalCount &&
               Objects.equals(data, dataItem.data) &&
               Objects.equals(createdAt, dataItem.createdAt);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(data, index, totalCount, createdAt);
    }
    
    @Override
    public String toString() {
        return String.format("DataItem{index=%d, totalCount=%d, dataType='%s', status=%s, dataSize=%d}", 
                           index, totalCount, dataType, status, dataSize);
    }
}
