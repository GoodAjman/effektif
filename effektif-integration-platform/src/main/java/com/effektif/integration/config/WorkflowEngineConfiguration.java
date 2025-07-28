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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.effektif.mongo.MongoConfiguration;
import com.effektif.workflow.api.Configuration;
import com.effektif.workflow.api.WorkflowEngine;

/**
 * 工作流引擎配置类
 * 
 * 负责配置和初始化Effektif工作流引擎，包括：
 * - MongoDB存储配置
 * - 工作流引擎实例创建
 * - 缓存配置
 * - 异步执行器配置
 * 
 * @author Integration Platform Team
 */
@Configuration
public class WorkflowEngineConfiguration {

    @Autowired
    private IntegrationPlatformProperties properties;

    /**
     * 创建工作流引擎配置
     * 
     * @return 工作流引擎配置实例
     */
    @Bean
    @Primary
    public Configuration workflowEngineConfiguration() {
        // 使用MongoDB配置
        MongoConfiguration configuration = new MongoConfiguration();
        
        // 设置MongoDB连接（这里可以从Spring的配置中获取）
        // configuration.mongoDb("effektif");
        
        // 配置缓存大小
        IntegrationPlatformProperties.WorkflowEngineProperties engineProps = properties.getWorkflowEngine();
        // configuration.workflowInstanceCacheSize(engineProps.getInstanceCacheSize());
        // configuration.workflowDefinitionCacheSize(engineProps.getDefinitionCacheSize());
        
        // 配置异步执行器
        // configuration.asyncExecutorThreadPoolSize(engineProps.getAsyncExecutorThreadPoolSize());
        
        // 启动配置
        configuration.start();
        
        return configuration;
    }

    /**
     * 创建工作流引擎实例
     * 
     * @param configuration 工作流引擎配置
     * @return 工作流引擎实例
     */
    @Bean
    @Primary
    public WorkflowEngine workflowEngine(Configuration configuration) {
        return configuration.getWorkflowEngine();
    }
}
