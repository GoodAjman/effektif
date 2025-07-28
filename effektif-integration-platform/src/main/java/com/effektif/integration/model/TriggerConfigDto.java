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
package com.effektif.integration.model;

import java.time.LocalDateTime;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 触发器配置DTO
 * 
 * 用于API接口的触发器配置数据传输对象。
 * 
 * @author Integration Platform Team
 */
@Schema(description = "触发器配置")
public class TriggerConfigDto {

    @Schema(description = "触发器ID", example = "payment-webhook-001")
    @NotBlank(message = "触发器ID不能为空")
    @Size(max = 64, message = "触发器ID长度不能超过64个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "触发器ID只能包含字母、数字、下划线和连字符")
    private String triggerId;

    @Schema(description = "触发器类型", example = "httpTrigger", 
            allowableValues = {"httpTrigger", "messageQueueTrigger", "scheduledTrigger", "databaseTrigger", "fileTrigger"})
    @NotBlank(message = "触发器类型不能为空")
    private String triggerType;

    @Schema(description = "关联的工作流ID", example = "payment-process-workflow")
    @NotBlank(message = "工作流ID不能为空")
    @Size(max = 64, message = "工作流ID长度不能超过64个字符")
    private String workflowId;

    @Schema(description = "触发器名称", example = "支付回调触发器")
    @NotBlank(message = "触发器名称不能为空")
    @Size(max = 128, message = "触发器名称长度不能超过128个字符")
    private String name;

    @Schema(description = "触发器描述", example = "处理第三方支付平台的回调通知")
    @Size(max = 1000, message = "触发器描述长度不能超过1000个字符")
    private String description;

    @Schema(description = "触发器配置", example = "{\"url\": \"/webhooks/payment\", \"method\": \"POST\", \"secretKey\": \"your-secret\"}")
    @NotNull(message = "触发器配置不能为空")
    private Map<String, Object> config;

    @Schema(description = "状态", example = "1", allowableValues = {"0", "1"})
    private Integer status = 1;

    @Schema(description = "创建时间", example = "2024-01-01T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2024-01-01T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

    @Schema(description = "创建人", example = "admin")
    @Size(max = 64, message = "创建人长度不能超过64个字符")
    private String createdBy;

    @Schema(description = "更新人", example = "admin")
    @Size(max = 64, message = "更新人长度不能超过64个字符")
    private String updatedBy;

    // 默认构造函数
    public TriggerConfigDto() {
    }

    // 构造函数
    public TriggerConfigDto(String triggerId, String triggerType, String workflowId, String name) {
        this.triggerId = triggerId;
        this.triggerType = triggerType;
        this.workflowId = workflowId;
        this.name = name;
    }

    // Getters and Setters
    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "TriggerConfigDto{" +
                "triggerId='" + triggerId + '\'' +
                ", triggerType='" + triggerType + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TriggerConfigDto that = (TriggerConfigDto) o;

        return triggerId != null ? triggerId.equals(that.triggerId) : that.triggerId == null;
    }

    @Override
    public int hashCode() {
        return triggerId != null ? triggerId.hashCode() : 0;
    }
}
