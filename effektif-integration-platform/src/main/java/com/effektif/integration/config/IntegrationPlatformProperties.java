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
package com.effektif.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 集成平台配置属性类
 * 
 * 统一管理集成平台的所有配置项，支持配置文件外部化和环境变量注入。
 * 
 * 配置文件示例 (application.yml):
 * <pre>
 * effektif:
 *   integration:
 *     platform:
 *       name: "Effektif Integration Platform 2.0"
 *       version: "2.0.0"
 *       triggers:
 *         http:
 *           enabled: true
 *           base-path: "/webhooks"
 *           max-payload-size: 10485760
 *         message-queue:
 *           enabled: true
 *           default-concurrency: 5
 *         scheduled:
 *           enabled: true
 *           thread-pool-size: 10
 *       security:
 *         enabled: true
 *         jwt-secret: "your-jwt-secret-key"
 *         token-expiration: 86400
 *       monitoring:
 *         enabled: true
 *         metrics-enabled: true
 *         health-check-enabled: true
 * </pre>
 * 
 * @author Integration Platform Team
 */
@ConfigurationProperties(prefix = "effektif.integration.platform")
@Validated
public class IntegrationPlatformProperties {

    /**
     * 平台名称
     */
    @NotBlank
    private String name = "Effektif Integration Platform 2.0";

    /**
     * 平台版本
     */
    @NotBlank
    private String version = "2.0.0";

    /**
     * 平台描述
     */
    private String description = "Enterprise-grade workflow integration platform";

    /**
     * 触发器配置
     */
    @Valid
    @NotNull
    private TriggerProperties triggers = new TriggerProperties();

    /**
     * 安全配置
     */
    @Valid
    @NotNull
    private SecurityProperties security = new SecurityProperties();

    /**
     * 监控配置
     */
    @Valid
    @NotNull
    private MonitoringProperties monitoring = new MonitoringProperties();

    /**
     * 工作流引擎配置
     */
    @Valid
    @NotNull
    private WorkflowEngineProperties workflowEngine = new WorkflowEngineProperties();

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TriggerProperties getTriggers() {
        return triggers;
    }

    public void setTriggers(TriggerProperties triggers) {
        this.triggers = triggers;
    }

    public SecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    public MonitoringProperties getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringProperties monitoring) {
        this.monitoring = monitoring;
    }

    public WorkflowEngineProperties getWorkflowEngine() {
        return workflowEngine;
    }

    public void setWorkflowEngine(WorkflowEngineProperties workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    /**
     * 触发器配置属性
     */
    public static class TriggerProperties {
        
        /**
         * HTTP触发器配置
         */
        @Valid
        @NotNull
        private HttpTriggerProperties http = new HttpTriggerProperties();

        /**
         * 消息队列触发器配置
         */
        @Valid
        @NotNull
        private MessageQueueTriggerProperties messageQueue = new MessageQueueTriggerProperties();

        /**
         * 定时触发器配置
         */
        @Valid
        @NotNull
        private ScheduledTriggerProperties scheduled = new ScheduledTriggerProperties();

        // Getters and Setters
        public HttpTriggerProperties getHttp() {
            return http;
        }

        public void setHttp(HttpTriggerProperties http) {
            this.http = http;
        }

        public MessageQueueTriggerProperties getMessageQueue() {
            return messageQueue;
        }

        public void setMessageQueue(MessageQueueTriggerProperties messageQueue) {
            this.messageQueue = messageQueue;
        }

        public ScheduledTriggerProperties getScheduled() {
            return scheduled;
        }

        public void setScheduled(ScheduledTriggerProperties scheduled) {
            this.scheduled = scheduled;
        }
    }

    /**
     * HTTP触发器配置属性
     */
    public static class HttpTriggerProperties {
        
        /**
         * 是否启用HTTP触发器
         */
        private boolean enabled = true;

        /**
         * HTTP触发器基础路径
         */
        @NotBlank
        private String basePath = "/webhooks";

        /**
         * 最大请求体大小（字节）
         */
        @Min(1024)
        private long maxPayloadSize = 10 * 1024 * 1024; // 10MB

        /**
         * 请求超时时间（秒）
         */
        @Min(1)
        private int requestTimeout = 30;

        /**
         * 是否启用签名验证
         */
        private boolean signatureVerificationEnabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public long getMaxPayloadSize() {
            return maxPayloadSize;
        }

        public void setMaxPayloadSize(long maxPayloadSize) {
            this.maxPayloadSize = maxPayloadSize;
        }

        public int getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(int requestTimeout) {
            this.requestTimeout = requestTimeout;
        }

        public boolean isSignatureVerificationEnabled() {
            return signatureVerificationEnabled;
        }

        public void setSignatureVerificationEnabled(boolean signatureVerificationEnabled) {
            this.signatureVerificationEnabled = signatureVerificationEnabled;
        }
    }

    /**
     * 消息队列触发器配置属性
     */
    public static class MessageQueueTriggerProperties {
        
        /**
         * 是否启用消息队列触发器
         */
        private boolean enabled = true;

        /**
         * 默认并发消费者数量
         */
        @Min(1)
        private int defaultConcurrency = 5;

        /**
         * 最大重试次数
         */
        @Min(0)
        private int maxRetries = 3;

        /**
         * 重试间隔（毫秒）
         */
        @Min(1000)
        private long retryInterval = 5000;

        /**
         * 是否启用消息去重
         */
        private boolean deduplicationEnabled = false;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultConcurrency() {
            return defaultConcurrency;
        }

        public void setDefaultConcurrency(int defaultConcurrency) {
            this.defaultConcurrency = defaultConcurrency;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
        }

        public boolean isDeduplicationEnabled() {
            return deduplicationEnabled;
        }

        public void setDeduplicationEnabled(boolean deduplicationEnabled) {
            this.deduplicationEnabled = deduplicationEnabled;
        }
    }

    /**
     * 定时触发器配置属性
     */
    public static class ScheduledTriggerProperties {
        
        /**
         * 是否启用定时触发器
         */
        private boolean enabled = true;

        /**
         * 线程池大小
         */
        @Min(1)
        private int threadPoolSize = 10;

        /**
         * 是否启用任务持久化
         */
        private boolean persistentEnabled = true;

        /**
         * 是否启用集群模式
         */
        private boolean clusterEnabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public boolean isPersistentEnabled() {
            return persistentEnabled;
        }

        public void setPersistentEnabled(boolean persistentEnabled) {
            this.persistentEnabled = persistentEnabled;
        }

        public boolean isClusterEnabled() {
            return clusterEnabled;
        }

        public void setClusterEnabled(boolean clusterEnabled) {
            this.clusterEnabled = clusterEnabled;
        }
    }

    /**
     * 安全配置属性
     */
    public static class SecurityProperties {
        
        /**
         * 是否启用安全认证
         */
        private boolean enabled = true;

        /**
         * JWT密钥
         */
        private String jwtSecret = "effektif-integration-platform-jwt-secret-key";

        /**
         * Token过期时间（秒）
         */
        @Min(60)
        private long tokenExpiration = 86400; // 24小时

        /**
         * 是否启用IP白名单
         */
        private boolean ipWhitelistEnabled = false;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getTokenExpiration() {
            return tokenExpiration;
        }

        public void setTokenExpiration(long tokenExpiration) {
            this.tokenExpiration = tokenExpiration;
        }

        public boolean isIpWhitelistEnabled() {
            return ipWhitelistEnabled;
        }

        public void setIpWhitelistEnabled(boolean ipWhitelistEnabled) {
            this.ipWhitelistEnabled = ipWhitelistEnabled;
        }
    }

    /**
     * 监控配置属性
     */
    public static class MonitoringProperties {
        
        /**
         * 是否启用监控
         */
        private boolean enabled = true;

        /**
         * 是否启用指标收集
         */
        private boolean metricsEnabled = true;

        /**
         * 是否启用健康检查
         */
        private boolean healthCheckEnabled = true;

        /**
         * 是否启用审计日志
         */
        private boolean auditLogEnabled = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMetricsEnabled() {
            return metricsEnabled;
        }

        public void setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
        }

        public boolean isHealthCheckEnabled() {
            return healthCheckEnabled;
        }

        public void setHealthCheckEnabled(boolean healthCheckEnabled) {
            this.healthCheckEnabled = healthCheckEnabled;
        }

        public boolean isAuditLogEnabled() {
            return auditLogEnabled;
        }

        public void setAuditLogEnabled(boolean auditLogEnabled) {
            this.auditLogEnabled = auditLogEnabled;
        }
    }

    /**
     * 工作流引擎配置属性
     */
    public static class WorkflowEngineProperties {
        
        /**
         * 工作流实例缓存大小
         */
        @Min(10)
        private int instanceCacheSize = 1000;

        /**
         * 工作流定义缓存大小
         */
        @Min(10)
        private int definitionCacheSize = 100;

        /**
         * 异步执行线程池大小
         */
        @Min(1)
        private int asyncExecutorThreadPoolSize = 20;

        // Getters and Setters
        public int getInstanceCacheSize() {
            return instanceCacheSize;
        }

        public void setInstanceCacheSize(int instanceCacheSize) {
            this.instanceCacheSize = instanceCacheSize;
        }

        public int getDefinitionCacheSize() {
            return definitionCacheSize;
        }

        public void setDefinitionCacheSize(int definitionCacheSize) {
            this.definitionCacheSize = definitionCacheSize;
        }

        public int getAsyncExecutorThreadPoolSize() {
            return asyncExecutorThreadPoolSize;
        }

        public void setAsyncExecutorThreadPoolSize(int asyncExecutorThreadPoolSize) {
            this.asyncExecutorThreadPoolSize = asyncExecutorThreadPoolSize;
        }
    }
}
