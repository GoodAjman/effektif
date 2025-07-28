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
package com.effektif.integration.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.effektif.integration.entity.TriggerConfig;
import com.effektif.integration.entity.TriggerExecutionLog;
import com.effektif.integration.model.TriggerConfigDto;
import com.effektif.integration.model.TriggerExecutionLogDto;
import com.effektif.integration.repository.TriggerConfigRepository;
import com.effektif.integration.repository.TriggerExecutionLogRepository;
import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.model.TriggerInstance;
import com.effektif.workflow.api.model.WorkflowId;
import com.effektif.workflow.api.workflowinstance.WorkflowInstance;
import com.effektif.integration.trigger.HttpTrigger;
import com.effektif.integration.trigger.impl.HttpTriggerProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 触发器服务类
 * 
 * 负责触发器的业务逻辑处理，包括：
 * - 触发器配置的CRUD操作
 * - HTTP触发器的请求处理
 * - 工作流的触发执行
 * - 执行日志的记录和查询
 * 
 * @author Integration Platform Team
 */
@Service
@Transactional
public class TriggerService {

    private static final Logger log = LoggerFactory.getLogger(TriggerService.class);

    @Autowired
    private TriggerConfigRepository triggerConfigRepository;

    @Autowired
    private TriggerExecutionLogRepository triggerExecutionLogRepository;

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpTriggerProcessor httpTriggerProcessor;

    /**
     * 创建触发器配置
     * 
     * @param triggerConfigDto 触发器配置DTO
     * @return 创建的触发器配置
     */
    public TriggerConfigDto createTrigger(TriggerConfigDto triggerConfigDto) {
        log.info("Creating trigger: {}", triggerConfigDto.getTriggerId());

        // 检查触发器ID是否已存在
        if (triggerConfigRepository.existsByTriggerId(triggerConfigDto.getTriggerId())) {
            throw new IllegalArgumentException("Trigger ID already exists: " + triggerConfigDto.getTriggerId());
        }

        // 验证工作流ID是否存在
        validateWorkflowId(triggerConfigDto.getWorkflowId());

        // 转换为实体并保存
        TriggerConfig entity = convertToEntity(triggerConfigDto);
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        
        TriggerConfig saved = triggerConfigRepository.save(entity);
        
        // 如果触发器启用，则启动监听
        if (saved.getStatus() == 1) {
            startTriggerListening(saved);
        }

        return convertToDto(saved);
    }

    /**
     * 更新触发器配置
     * 
     * @param triggerConfigDto 触发器配置DTO
     * @return 更新后的触发器配置
     */
    public TriggerConfigDto updateTrigger(TriggerConfigDto triggerConfigDto) {
        log.info("Updating trigger: {}", triggerConfigDto.getTriggerId());

        TriggerConfig existing = triggerConfigRepository.findByTriggerId(triggerConfigDto.getTriggerId());
        if (existing == null) {
            throw new IllegalArgumentException("Trigger not found: " + triggerConfigDto.getTriggerId());
        }

        // 验证工作流ID是否存在
        validateWorkflowId(triggerConfigDto.getWorkflowId());

        // 停止旧的监听
        if (existing.getStatus() == 1) {
            stopTriggerListening(existing);
        }

        // 更新实体
        updateEntityFromDto(existing, triggerConfigDto);
        existing.setUpdatedTime(LocalDateTime.now());
        
        TriggerConfig saved = triggerConfigRepository.save(existing);
        
        // 如果触发器启用，则启动新的监听
        if (saved.getStatus() == 1) {
            startTriggerListening(saved);
        }

        return convertToDto(saved);
    }

    /**
     * 获取触发器配置
     * 
     * @param triggerId 触发器ID
     * @return 触发器配置
     */
    @Transactional(readOnly = true)
    public TriggerConfigDto getTrigger(String triggerId) {
        TriggerConfig entity = triggerConfigRepository.findByTriggerId(triggerId);
        return entity != null ? convertToDto(entity) : null;
    }

    /**
     * 获取触发器列表
     * 
     * @param triggerType 触发器类型
     * @param status 状态
     * @param page 页码
     * @param size 页大小
     * @return 触发器配置列表
     */
    @Transactional(readOnly = true)
    public List<TriggerConfigDto> getTriggers(String triggerType, Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdTime").descending());
        Page<TriggerConfig> entities;

        if (triggerType != null && status != null) {
            entities = triggerConfigRepository.findByTriggerTypeAndStatus(triggerType, status, pageable);
        } else if (triggerType != null) {
            entities = triggerConfigRepository.findByTriggerType(triggerType, pageable);
        } else if (status != null) {
            entities = triggerConfigRepository.findByStatus(status, pageable);
        } else {
            entities = triggerConfigRepository.findAll(pageable);
        }

        return entities.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 删除触发器配置
     * 
     * @param triggerId 触发器ID
     * @return 是否删除成功
     */
    public boolean deleteTrigger(String triggerId) {
        log.info("Deleting trigger: {}", triggerId);

        TriggerConfig entity = triggerConfigRepository.findByTriggerId(triggerId);
        if (entity == null) {
            return false;
        }

        // 停止监听
        if (entity.getStatus() == 1) {
            stopTriggerListening(entity);
        }

        // 删除相关的执行日志
        triggerExecutionLogRepository.deleteByTriggerId(triggerId);
        
        // 删除触发器配置
        triggerConfigRepository.delete(entity);
        
        return true;
    }

    /**
     * 更新触发器状态
     * 
     * @param triggerId 触发器ID
     * @param enabled 是否启用
     * @return 是否更新成功
     */
    public boolean updateTriggerStatus(String triggerId, boolean enabled) {
        log.info("Updating trigger status: {} -> {}", triggerId, enabled);

        TriggerConfig entity = triggerConfigRepository.findByTriggerId(triggerId);
        if (entity == null) {
            return false;
        }

        int newStatus = enabled ? 1 : 0;
        if (entity.getStatus() == newStatus) {
            return true; // 状态未变化
        }

        // 更新状态
        if (enabled) {
            startTriggerListening(entity);
        } else {
            stopTriggerListening(entity);
        }

        entity.setStatus(newStatus);
        entity.setUpdatedTime(LocalDateTime.now());
        triggerConfigRepository.save(entity);

        return true;
    }

    /**
     * 处理HTTP触发器请求
     * 
     * @param triggerId 触发器ID
     * @param request HTTP请求
     * @return 处理结果
     */
    public Map<String, Object> processHttpTrigger(String triggerId, HttpServletRequest request) {
        log.info("Processing HTTP trigger: {}", triggerId);

        TriggerConfig triggerConfig = triggerConfigRepository.findByTriggerId(triggerId);
        if (triggerConfig == null) {
            throw new IllegalArgumentException("Trigger not found: " + triggerId);
        }

        if (triggerConfig.getStatus() != 1) {
            throw new IllegalArgumentException("Trigger is disabled: " + triggerId);
        }

        if (!"httpTrigger".equals(triggerConfig.getTriggerType())) {
            throw new IllegalArgumentException("Not an HTTP trigger: " + triggerId);
        }

        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 从配置中构建HTTP触发器对象
            HttpTrigger httpTrigger = buildHttpTriggerFromConfig(triggerConfig);

            // 验证请求
            if (!httpTriggerProcessor.validateRequest(httpTrigger, request)) {
                throw new SecurityException("HTTP request validation failed");
            }

            // 解析请求数据
            Map<String, Object> requestData = httpTriggerProcessor.parseRequestData(request);

            // 创建触发器实例
            TriggerInstance triggerInstance = new TriggerInstance();
            triggerInstance.setWorkflowId(new WorkflowId(triggerConfig.getWorkflowId()));
            
            // 设置触发数据
            for (Map.Entry<String, Object> entry : requestData.entrySet()) {
                triggerInstance.data(entry.getKey(), entry.getValue());
            }

            // 启动工作流
            WorkflowInstance workflowInstance = workflowEngine.start(triggerInstance);

            // 记录执行日志
            recordExecutionLog(triggerId, executionId, workflowInstance.getId().getInternal(), 
                             requestData, 1, null, System.currentTimeMillis() - startTime);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionId", executionId);
            result.put("workflowInstanceId", workflowInstance.getId().getInternal());
            result.put("message", "Workflow started successfully");

            return result;

        } catch (Exception e) {
            log.error("Error processing HTTP trigger: " + triggerId, e);
            
            // 记录错误日志
            recordExecutionLog(triggerId, executionId, null, null, 0, e.getMessage(), 
                             System.currentTimeMillis() - startTime);

            throw e;
        }
    }

    /**
     * 手动执行触发器
     * 
     * @param triggerId 触发器ID
     * @param data 触发数据
     * @return 执行结果
     */
    public Map<String, Object> executeTrigger(String triggerId, Map<String, Object> data) {
        log.info("Manually executing trigger: {}", triggerId);

        TriggerConfig triggerConfig = triggerConfigRepository.findByTriggerId(triggerId);
        if (triggerConfig == null) {
            throw new IllegalArgumentException("Trigger not found: " + triggerId);
        }

        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 创建触发器实例
            TriggerInstance triggerInstance = new TriggerInstance();
            triggerInstance.setWorkflowId(new WorkflowId(triggerConfig.getWorkflowId()));
            
            // 设置触发数据
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    triggerInstance.data(entry.getKey(), entry.getValue());
                }
            }

            // 启动工作流
            WorkflowInstance workflowInstance = workflowEngine.start(triggerInstance);

            // 记录执行日志
            recordExecutionLog(triggerId, executionId, workflowInstance.getId().getInternal(), 
                             data, 1, null, System.currentTimeMillis() - startTime);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionId", executionId);
            result.put("workflowInstanceId", workflowInstance.getId().getInternal());
            result.put("message", "Workflow started successfully");

            return result;

        } catch (Exception e) {
            log.error("Error executing trigger: " + triggerId, e);
            
            // 记录错误日志
            recordExecutionLog(triggerId, executionId, null, data, 0, e.getMessage(), 
                             System.currentTimeMillis() - startTime);

            throw e;
        }
    }

    /**
     * 获取触发器执行日志
     * 
     * @param triggerId 触发器ID
     * @param page 页码
     * @param size 页大小
     * @return 执行日志列表
     */
    @Transactional(readOnly = true)
    public List<TriggerExecutionLogDto> getTriggerExecutionLogs(String triggerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("executionTime").descending());
        Page<TriggerExecutionLog> logs = triggerExecutionLogRepository.findByTriggerId(triggerId, pageable);
        
        return logs.getContent().stream()
                .map(this::convertLogToDto)
                .collect(Collectors.toList());
    }

    // 私有辅助方法

    private void validateWorkflowId(String workflowId) {
        // 这里可以验证工作流ID是否存在
        // 暂时跳过验证
    }

    private void startTriggerListening(TriggerConfig triggerConfig) {
        // 根据触发器类型启动相应的监听器
        log.info("Starting trigger listening: {} ({})", triggerConfig.getTriggerId(), triggerConfig.getTriggerType());
        
        switch (triggerConfig.getTriggerType()) {
            case "httpTrigger":
                // HTTP触发器不需要启动监听器，通过REST端点处理
                break;
            case "messageQueueTrigger":
                // 启动消息队列监听器
                startMessageQueueListener(triggerConfig);
                break;
            case "scheduledTrigger":
                // 启动定时任务
                startScheduledTask(triggerConfig);
                break;
            default:
                log.warn("Unknown trigger type: {}", triggerConfig.getTriggerType());
        }
    }

    private void stopTriggerListening(TriggerConfig triggerConfig) {
        // 根据触发器类型停止相应的监听器
        log.info("Stopping trigger listening: {} ({})", triggerConfig.getTriggerId(), triggerConfig.getTriggerType());
        
        switch (triggerConfig.getTriggerType()) {
            case "httpTrigger":
                // HTTP触发器不需要停止监听器
                break;
            case "messageQueueTrigger":
                // 停止消息队列监听器
                stopMessageQueueListener(triggerConfig);
                break;
            case "scheduledTrigger":
                // 停止定时任务
                stopScheduledTask(triggerConfig);
                break;
            default:
                log.warn("Unknown trigger type: {}", triggerConfig.getTriggerType());
        }
    }

    private void startMessageQueueListener(TriggerConfig triggerConfig) {
        // TODO: 实现消息队列监听器启动逻辑
        log.info("Starting message queue listener for trigger: {}", triggerConfig.getTriggerId());
    }

    private void stopMessageQueueListener(TriggerConfig triggerConfig) {
        // TODO: 实现消息队列监听器停止逻辑
        log.info("Stopping message queue listener for trigger: {}", triggerConfig.getTriggerId());
    }

    private void startScheduledTask(TriggerConfig triggerConfig) {
        // TODO: 实现定时任务启动逻辑
        log.info("Starting scheduled task for trigger: {}", triggerConfig.getTriggerId());
    }

    private void stopScheduledTask(TriggerConfig triggerConfig) {
        // TODO: 实现定时任务停止逻辑
        log.info("Stopping scheduled task for trigger: {}", triggerConfig.getTriggerId());
    }

    private void recordExecutionLog(String triggerId, String executionId, String workflowInstanceId,
                                  Map<String, Object> triggerData, int status, String errorMessage, long duration) {
        try {
            TriggerExecutionLog log = new TriggerExecutionLog();
            log.setTriggerId(triggerId);
            log.setExecutionId(executionId);
            log.setWorkflowInstanceId(workflowInstanceId);
            log.setExecutionStatus(status);
            log.setErrorMessage(errorMessage);
            log.setExecutionTime(LocalDateTime.now());
            log.setDurationMs(duration);
            
            if (triggerData != null) {
                log.setTriggerData(objectMapper.writeValueAsString(triggerData));
            }
            
            triggerExecutionLogRepository.save(log);
        } catch (Exception e) {
            log.error("Error recording execution log", e);
        }
    }

    private TriggerConfig convertToEntity(TriggerConfigDto dto) {
        TriggerConfig entity = new TriggerConfig();
        updateEntityFromDto(entity, dto);
        return entity;
    }

    private void updateEntityFromDto(TriggerConfig entity, TriggerConfigDto dto) {
        entity.setTriggerId(dto.getTriggerId());
        entity.setTriggerType(dto.getTriggerType());
        entity.setWorkflowId(dto.getWorkflowId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getUpdatedBy());
        
        try {
            entity.setConfigJson(objectMapper.writeValueAsString(dto.getConfig()));
        } catch (Exception e) {
            throw new RuntimeException("Error serializing trigger config", e);
        }
    }

    private TriggerConfigDto convertToDto(TriggerConfig entity) {
        TriggerConfigDto dto = new TriggerConfigDto();
        dto.setTriggerId(entity.getTriggerId());
        dto.setTriggerType(entity.getTriggerType());
        dto.setWorkflowId(entity.getWorkflowId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getUpdatedTime());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        
        try {
            if (entity.getConfigJson() != null) {
                dto.setConfig(objectMapper.readValue(entity.getConfigJson(), Map.class));
            }
        } catch (Exception e) {
            log.error("Error deserializing trigger config", e);
        }
        
        return dto;
    }

    private TriggerExecutionLogDto convertLogToDto(TriggerExecutionLog entity) {
        TriggerExecutionLogDto dto = new TriggerExecutionLogDto();
        dto.setId(entity.getId());
        dto.setTriggerId(entity.getTriggerId());
        dto.setWorkflowInstanceId(entity.getWorkflowInstanceId());
        dto.setExecutionId(entity.getExecutionId());
        dto.setExecutionStatus(entity.getExecutionStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setExecutionTime(entity.getExecutionTime());
        dto.setDurationMs(entity.getDurationMs());
        
        try {
            if (entity.getTriggerData() != null) {
                dto.setTriggerData(objectMapper.readValue(entity.getTriggerData(), Map.class));
            }
        } catch (Exception e) {
            log.error("Error deserializing trigger data", e);
        }
        
        return dto;
    }

    /**
     * 从触发器配置构建HttpTrigger对象
     */
    private HttpTrigger buildHttpTriggerFromConfig(TriggerConfig triggerConfig) {
        try {
            Map<String, Object> config = objectMapper.readValue(triggerConfig.getConfigJson(), Map.class);

            HttpTrigger httpTrigger = new HttpTrigger();
            httpTrigger.url((String) config.get("url"));
            httpTrigger.method((String) config.getOrDefault("method", "POST"));
            httpTrigger.contentType((String) config.getOrDefault("contentType", "application/json"));
            httpTrigger.secretKey((String) config.get("secretKey"));
            httpTrigger.async((Boolean) config.getOrDefault("async", true));
            httpTrigger.enableSignatureVerification((Boolean) config.getOrDefault("enableSignatureVerification", true));
            httpTrigger.signatureAlgorithm((String) config.getOrDefault("signatureAlgorithm", "HmacSHA256"));
            httpTrigger.signatureHeader((String) config.getOrDefault("signatureHeader", "X-Signature"));
            httpTrigger.timeoutSeconds((Integer) config.getOrDefault("timeoutSeconds", 30));

            // 处理IP白名单
            String allowedIpsStr = (String) config.get("allowedIps");
            if (allowedIpsStr != null && !allowedIpsStr.trim().isEmpty()) {
                String[] allowedIps = allowedIpsStr.split(",");
                for (int i = 0; i < allowedIps.length; i++) {
                    allowedIps[i] = allowedIps[i].trim();
                }
                httpTrigger.allowedIps(allowedIps);
            }

            // 处理请求头
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) config.get("headers");
            if (headers != null) {
                httpTrigger.headers(headers);
            }

            return httpTrigger;
        } catch (Exception e) {
            log.error("Error building HttpTrigger from config", e);
            throw new RuntimeException("Invalid trigger configuration", e);
        }
    }
}
