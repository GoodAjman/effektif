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
package com.effektif.integration.trigger;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列触发器配置类
 * 
 * 用于配置消息队列触发器的各种参数，包括：
 * - 队列和交换机配置
 * - 消息处理和确认机制
 * - 重试和死信队列处理
 * - 消息去重和批量处理
 * 
 * @author Integration Platform Team
 */
public class MessageQueueTrigger {

    /** 队列名称 */
    private String queueName;
    
    /** 交换机名称 */
    private String exchangeName;
    
    /** 路由键 */
    private String routingKey;
    
    /** 连接工厂Bean名称 */
    private String connectionFactory;
    
    /** 并发消费者数量，默认为1 */
    private int concurrency = 1;
    
    /** 队列是否持久化，默认为true */
    private boolean durable = true;
    
    /** 是否自动确认消息，默认为false */
    private boolean autoAck = false;
    
    /** 消息类型，用于反序列化 */
    private String messageType = "java.lang.String";
    
    /** 最大重试次数，默认为3 */
    private int maxRetries = 3;
    
    /** 重试间隔（毫秒），默认为5000 */
    private long retryInterval = 5000;
    
    /** 死信队列名称 */
    private String deadLetterQueue;
    
    /** 消息过滤表达式（SpEL） */
    private String messageFilter;
    
    /** 批量处理大小，默认为1（不批量） */
    private int batchSize = 1;
    
    /** 批量处理超时时间（毫秒），默认为1000 */
    private long batchTimeout = 1000;
    
    /** 消费者标签前缀 */
    private String consumerTagPrefix;
    
    /** 是否启用消息去重，默认为false */
    private boolean enableDeduplication = false;
    
    /** 去重键表达式（SpEL） */
    private String deduplicationKey;
    
    /** 去重缓存过期时间（秒），默认为3600 */
    private int deduplicationExpireSeconds = 3600;

    /** 输出变量映射 */
    private Map<String, String> outputs;

    // 默认构造函数
    public MessageQueueTrigger() {
    }

    // Getter and Setter methods with fluent API
    public String getQueueName() {
        return queueName;
    }

    public MessageQueueTrigger queueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public MessageQueueTrigger exchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
        return this;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public MessageQueueTrigger routingKey(String routingKey) {
        this.routingKey = routingKey;
        return this;
    }

    public String getConnectionFactory() {
        return connectionFactory;
    }

    public MessageQueueTrigger connectionFactory(String connectionFactory) {
        this.connectionFactory = connectionFactory;
        return this;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public MessageQueueTrigger concurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public boolean isDurable() {
        return durable;
    }

    public MessageQueueTrigger durable(boolean durable) {
        this.durable = durable;
        return this;
    }

    public boolean isAutoAck() {
        return autoAck;
    }

    public MessageQueueTrigger autoAck(boolean autoAck) {
        this.autoAck = autoAck;
        return this;
    }

    public String getMessageType() {
        return messageType;
    }

    public MessageQueueTrigger messageType(String messageType) {
        this.messageType = messageType;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public MessageQueueTrigger maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public MessageQueueTrigger retryInterval(long retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public String getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public MessageQueueTrigger deadLetterQueue(String deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
        return this;
    }

    public String getMessageFilter() {
        return messageFilter;
    }

    public MessageQueueTrigger messageFilter(String messageFilter) {
        this.messageFilter = messageFilter;
        return this;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public MessageQueueTrigger batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public long getBatchTimeout() {
        return batchTimeout;
    }

    public MessageQueueTrigger batchTimeout(long batchTimeout) {
        this.batchTimeout = batchTimeout;
        return this;
    }

    public String getConsumerTagPrefix() {
        return consumerTagPrefix;
    }

    public MessageQueueTrigger consumerTagPrefix(String consumerTagPrefix) {
        this.consumerTagPrefix = consumerTagPrefix;
        return this;
    }

    public boolean isEnableDeduplication() {
        return enableDeduplication;
    }

    public MessageQueueTrigger enableDeduplication(boolean enableDeduplication) {
        this.enableDeduplication = enableDeduplication;
        return this;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public MessageQueueTrigger deduplicationKey(String deduplicationKey) {
        this.deduplicationKey = deduplicationKey;
        return this;
    }

    public int getDeduplicationExpireSeconds() {
        return deduplicationExpireSeconds;
    }

    public MessageQueueTrigger deduplicationExpireSeconds(int deduplicationExpireSeconds) {
        this.deduplicationExpireSeconds = deduplicationExpireSeconds;
        return this;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public MessageQueueTrigger outputs(Map<String, String> outputs) {
        this.outputs = outputs;
        return this;
    }

    public MessageQueueTrigger output(String key, String outputVariableId) {
        if (outputs == null) {
            outputs = new HashMap<>();
        }
        outputs.put(key, outputVariableId);
        return this;
    }

    @Override
    public String toString() {
        return "MessageQueueTrigger{" +
                "queueName='" + queueName + '\'' +
                ", exchangeName='" + exchangeName + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", concurrency=" + concurrency +
                ", durable=" + durable +
                ", autoAck=" + autoAck +
                '}';
    }
}
