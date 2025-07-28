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
package com.effektif.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.effektif.integration.config.IntegrationPlatformProperties;

/**
 * Effektif集成平台Spring Boot应用程序主类
 * 
 * 这是基于Spring Boot的集成平台应用程序，提供以下核心功能：
 * 
 * 1. **工作流引擎集成**
 *    - 集成Effektif工作流引擎
 *    - 支持工作流的部署、启动、管理
 *    - 提供REST API接口
 * 
 * 2. **多种触发器支持**
 *    - HTTP触发器：支持Webhook集成
 *    - 消息队列触发器：支持RabbitMQ、Kafka等
 *    - 定时触发器：支持Cron表达式和固定间隔
 *    - 数据库触发器：支持数据变更监听
 *    - 文件触发器：支持文件系统监听
 * 
 * 3. **企业级特性**
 *    - 高可用和集群支持
 *    - 监控和指标收集
 *    - 安全认证和授权
 *    - 配置管理和外部化
 *    - 日志记录和审计
 * 
 * 4. **开发和运维友好**
 *    - 健康检查和优雅关闭
 *    - 自动配置和约定优于配置
 *    - 完整的API文档
 *    - 单元测试和集成测试
 * 
 * 启动参数示例：
 * <pre>
 * java -jar effektif-integration-platform.jar \
 *   --spring.profiles.active=production \
 *   --server.port=8080 \
 *   --effektif.mongodb.uri=mongodb://localhost:27017/effektif \
 *   --effektif.rabbitmq.host=localhost \
 *   --effektif.rabbitmq.port=5672
 * </pre>
 * 
 * 环境变量配置：
 * <pre>
 * SPRING_PROFILES_ACTIVE=production
 * SERVER_PORT=8080
 * EFFEKTIF_MONGODB_URI=mongodb://localhost:27017/effektif
 * EFFEKTIF_RABBITMQ_HOST=localhost
 * EFFEKTIF_RABBITMQ_PORT=5672
 * </pre>
 * 
 * @author Integration Platform Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication(scanBasePackages = {
    "com.effektif.integration",
    "com.effektif.workflow.impl",
    "com.effektif.mongo"
})
@EnableConfigurationProperties({
    IntegrationPlatformProperties.class
})
@EnableAsync
@EnableScheduling
public class IntegrationPlatformApplication {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("spring.application.name", "effektif-integration-platform");
        System.setProperty("spring.application.version", getVersion());
        
        // 启动Spring Boot应用
        SpringApplication app = new SpringApplication(IntegrationPlatformApplication.class);
        
        // 设置默认配置文件
        app.setDefaultProperties(getDefaultProperties());
        
        // 启动应用
        app.run(args);
    }

    /**
     * 获取应用版本号
     * 
     * @return 版本号
     */
    private static String getVersion() {
        Package pkg = IntegrationPlatformApplication.class.getPackage();
        return pkg != null && pkg.getImplementationVersion() != null 
            ? pkg.getImplementationVersion() 
            : "1.0.0-SNAPSHOT";
    }

    /**
     * 获取默认配置属性
     * 
     * @return 默认配置属性Map
     */
    private static java.util.Properties getDefaultProperties() {
        java.util.Properties properties = new java.util.Properties();
        
        // 服务器配置
        properties.setProperty("server.port", "8080");
        properties.setProperty("server.servlet.context-path", "/api");
        properties.setProperty("server.compression.enabled", "true");
        properties.setProperty("server.compression.mime-types", 
            "text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json");
        
        // 管理端点配置
        properties.setProperty("management.endpoints.web.exposure.include", "health,info,metrics,prometheus");
        properties.setProperty("management.endpoint.health.show-details", "when-authorized");
        properties.setProperty("management.metrics.export.prometheus.enabled", "true");
        
        // 日志配置
        properties.setProperty("logging.level.com.effektif", "INFO");
        properties.setProperty("logging.level.org.springframework.amqp", "WARN");
        properties.setProperty("logging.pattern.console", 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        
        // Jackson配置
        properties.setProperty("spring.jackson.serialization.write-dates-as-timestamps", "false");
        properties.setProperty("spring.jackson.serialization.fail-on-empty-beans", "false");
        properties.setProperty("spring.jackson.deserialization.fail-on-unknown-properties", "false");
        
        // JPA配置
        properties.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        properties.setProperty("spring.jpa.show-sql", "false");
        properties.setProperty("spring.jpa.properties.hibernate.dialect", 
            "org.hibernate.dialect.MySQL8Dialect");
        
        // 连接池配置
        properties.setProperty("spring.datasource.hikari.maximum-pool-size", "20");
        properties.setProperty("spring.datasource.hikari.minimum-idle", "5");
        properties.setProperty("spring.datasource.hikari.connection-timeout", "30000");
        properties.setProperty("spring.datasource.hikari.idle-timeout", "600000");
        properties.setProperty("spring.datasource.hikari.max-lifetime", "1800000");
        
        // RabbitMQ配置
        properties.setProperty("spring.rabbitmq.listener.simple.retry.enabled", "true");
        properties.setProperty("spring.rabbitmq.listener.simple.retry.max-attempts", "3");
        properties.setProperty("spring.rabbitmq.listener.simple.retry.initial-interval", "5000");
        properties.setProperty("spring.rabbitmq.listener.simple.retry.multiplier", "2.0");
        properties.setProperty("spring.rabbitmq.listener.simple.retry.max-interval", "30000");
        
        // Quartz配置
        properties.setProperty("spring.quartz.job-store-type", "jdbc");
        properties.setProperty("spring.quartz.jdbc.initialize-schema", "embedded");
        properties.setProperty("spring.quartz.properties.org.quartz.scheduler.instanceName", 
            "EffektifIntegrationPlatformScheduler");
        properties.setProperty("spring.quartz.properties.org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("spring.quartz.properties.org.quartz.jobStore.class", 
            "org.quartz.impl.jdbcjobstore.JobStoreTX");
        properties.setProperty("spring.quartz.properties.org.quartz.jobStore.driverDelegateClass", 
            "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        properties.setProperty("spring.quartz.properties.org.quartz.jobStore.tablePrefix", "QRTZ_");
        properties.setProperty("spring.quartz.properties.org.quartz.jobStore.isClustered", "true");
        properties.setProperty("spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval", "20000");
        
        // 线程池配置
        properties.setProperty("spring.task.execution.pool.core-size", "8");
        properties.setProperty("spring.task.execution.pool.max-size", "32");
        properties.setProperty("spring.task.execution.pool.queue-capacity", "1000");
        properties.setProperty("spring.task.execution.thread-name-prefix", "effektif-async-");
        
        // 安全配置
        properties.setProperty("spring.security.user.name", "admin");
        properties.setProperty("spring.security.user.password", "admin123");
        properties.setProperty("spring.security.user.roles", "ADMIN");
        
        return properties;
    }
}
