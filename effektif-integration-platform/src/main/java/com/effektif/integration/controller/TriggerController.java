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
package com.effektif.integration.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.effektif.integration.model.TriggerConfigDto;
import com.effektif.integration.model.TriggerExecutionLogDto;
import com.effektif.integration.service.TriggerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 触发器管理REST控制器
 * 
 * 提供触发器的CRUD操作和HTTP触发器的Webhook端点，包括：
 * - 触发器配置管理
 * - HTTP Webhook处理
 * - 触发器执行日志查询
 * - 触发器状态管理
 * 
 * @author Integration Platform Team
 */
@RestController
@RequestMapping("/api/triggers")
@Tag(name = "Trigger Management", description = "触发器管理API")
public class TriggerController {

    private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

    @Autowired
    private TriggerService triggerService;

    /**
     * 创建触发器配置
     * 
     * @param triggerConfig 触发器配置
     * @return 创建的触发器配置
     */
    @PostMapping
    @Operation(summary = "创建触发器", description = "创建新的触发器配置")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "触发器创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "409", description = "触发器ID已存在")
    })
    public ResponseEntity<TriggerConfigDto> createTrigger(
            @Valid @RequestBody TriggerConfigDto triggerConfig) {
        
        log.info("Creating trigger: {}", triggerConfig.getTriggerId());
        
        try {
            TriggerConfigDto created = triggerService.createTrigger(triggerConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger configuration: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 更新触发器配置
     * 
     * @param triggerId 触发器ID
     * @param triggerConfig 触发器配置
     * @return 更新后的触发器配置
     */
    @PutMapping("/{triggerId}")
    @Operation(summary = "更新触发器", description = "更新指定的触发器配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "触发器更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "404", description = "触发器不存在")
    })
    public ResponseEntity<TriggerConfigDto> updateTrigger(
            @PathVariable String triggerId,
            @Valid @RequestBody TriggerConfigDto triggerConfig) {
        
        log.info("Updating trigger: {}", triggerId);
        
        try {
            triggerConfig.setTriggerId(triggerId);
            TriggerConfigDto updated = triggerService.updateTrigger(triggerConfig);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger configuration: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取触发器配置
     * 
     * @param triggerId 触发器ID
     * @return 触发器配置
     */
    @GetMapping("/{triggerId}")
    @Operation(summary = "获取触发器", description = "根据ID获取触发器配置")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "触发器不存在")
    })
    public ResponseEntity<TriggerConfigDto> getTrigger(@PathVariable String triggerId) {
        log.debug("Getting trigger: {}", triggerId);
        
        try {
            TriggerConfigDto trigger = triggerService.getTrigger(triggerId);
            return trigger != null ? ResponseEntity.ok(trigger) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取触发器列表
     * 
     * @param triggerType 触发器类型（可选）
     * @param status 状态（可选）
     * @param page 页码
     * @param size 页大小
     * @return 触发器配置列表
     */
    @GetMapping
    @Operation(summary = "获取触发器列表", description = "分页获取触发器配置列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    public ResponseEntity<List<TriggerConfigDto>> getTriggers(
            @RequestParam(required = false) @Parameter(description = "触发器类型") String triggerType,
            @RequestParam(required = false) @Parameter(description = "状态") Integer status,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "页大小") int size) {
        
        log.debug("Getting triggers: type={}, status={}, page={}, size={}", triggerType, status, page, size);
        
        try {
            List<TriggerConfigDto> triggers = triggerService.getTriggers(triggerType, status, page, size);
            return ResponseEntity.ok(triggers);
        } catch (Exception e) {
            log.error("Error getting triggers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除触发器配置
     * 
     * @param triggerId 触发器ID
     * @return 删除结果
     */
    @DeleteMapping("/{triggerId}")
    @Operation(summary = "删除触发器", description = "删除指定的触发器配置")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "触发器不存在")
    })
    public ResponseEntity<Void> deleteTrigger(@PathVariable String triggerId) {
        log.info("Deleting trigger: {}", triggerId);
        
        try {
            boolean deleted = triggerService.deleteTrigger(triggerId);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 启用/禁用触发器
     * 
     * @param triggerId 触发器ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PutMapping("/{triggerId}/status")
    @Operation(summary = "更新触发器状态", description = "启用或禁用触发器")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功"),
        @ApiResponse(responseCode = "404", description = "触发器不存在")
    })
    public ResponseEntity<Void> updateTriggerStatus(
            @PathVariable String triggerId,
            @RequestParam boolean enabled) {
        
        log.info("Updating trigger status: {} -> {}", triggerId, enabled);
        
        try {
            boolean updated = triggerService.updateTriggerStatus(triggerId, enabled);
            return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating trigger status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * HTTP Webhook端点 - 处理HTTP触发器请求
     * 
     * @param triggerId 触发器ID
     * @param request HTTP请求
     * @return 处理结果
     */
    @PostMapping("/http/{triggerId}")
    @Operation(summary = "HTTP Webhook", description = "HTTP触发器Webhook端点")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "处理成功"),
        @ApiResponse(responseCode = "400", description = "请求无效"),
        @ApiResponse(responseCode = "401", description = "认证失败"),
        @ApiResponse(responseCode = "404", description = "触发器不存在"),
        @ApiResponse(responseCode = "500", description = "处理失败")
    })
    public ResponseEntity<Map<String, Object>> handleHttpTrigger(
            @PathVariable String triggerId,
            HttpServletRequest request) {
        
        log.info("Processing HTTP trigger: {} from {}", triggerId, request.getRemoteAddr());
        
        try {
            Map<String, Object> result = triggerService.processHttpTrigger(triggerId, request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid HTTP trigger request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            log.warn("HTTP trigger authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("Error processing HTTP trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
    @GetMapping("/{triggerId}/logs")
    @Operation(summary = "获取触发器执行日志", description = "分页获取触发器的执行日志")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "触发器不存在")
    })
    public ResponseEntity<List<TriggerExecutionLogDto>> getTriggerExecutionLogs(
            @PathVariable String triggerId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "页大小") int size) {
        
        log.debug("Getting execution logs for trigger: {}", triggerId);
        
        try {
            List<TriggerExecutionLogDto> logs = triggerService.getTriggerExecutionLogs(triggerId, page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Error getting trigger execution logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 手动触发工作流
     * 
     * @param triggerId 触发器ID
     * @param data 触发数据
     * @return 触发结果
     */
    @PostMapping("/{triggerId}/execute")
    @Operation(summary = "手动触发工作流", description = "手动执行指定的触发器")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "触发成功"),
        @ApiResponse(responseCode = "404", description = "触发器不存在"),
        @ApiResponse(responseCode = "500", description = "触发失败")
    })
    public ResponseEntity<Map<String, Object>> executeTrigger(
            @PathVariable String triggerId,
            @RequestBody(required = false) Map<String, Object> data) {
        
        log.info("Manually executing trigger: {}", triggerId);
        
        try {
            Map<String, Object> result = triggerService.executeTrigger(triggerId, data);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trigger execution request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error executing trigger", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
