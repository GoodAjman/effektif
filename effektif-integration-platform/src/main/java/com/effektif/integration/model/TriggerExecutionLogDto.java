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

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 触发器执行日志DTO
 * 
 * 用于API接口的触发器执行日志数据传输对象。
 * 
 * @author Integration Platform Team
 */
@Schema(description = "触发器执行日志")
public class TriggerExecutionLogDto {

    @Schema(description = "日志ID", example = "1")
    private Long id;

    @Schema(description = "触发器ID", example = "payment-webhook-001")
    private String triggerId;

    @Schema(description = "工作流实例ID", example = "wf-instance-12345")
    private String workflowInstanceId;

    @Schema(description = "执行ID", example = "exec-uuid-67890")
    private String executionId;

    @Schema(description = "触发数据", example = "{\"orderId\": \"12345\", \"amount\": 100.00}")
    private Map<String, Object> triggerData;

    @Schema(description = "执行状态", example = "1", allowableValues = {"0", "1", "2"})
    private Integer executionStatus;

    @Schema(description = "错误信息", example = "网络连接超时")
    private String errorMessage;

    @Schema(description = "执行时间", example = "2024-01-01T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executionTime;

    @Schema(description = "执行耗时（毫秒）", example = "1500")
    private Long durationMs;

    // 默认构造函数
    public TriggerExecutionLogDto() {
    }

    // 构造函数
    public TriggerExecutionLogDto(String triggerId, String executionId, Integer executionStatus) {
        this.triggerId = triggerId;
        this.executionId = executionId;
        this.executionStatus = executionStatus;
        this.executionTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(String triggerId) {
        this.triggerId = triggerId;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public Map<String, Object> getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(Map<String, Object> triggerData) {
        this.triggerData = triggerData;
    }

    public Integer getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(Integer executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * 获取执行状态描述
     * 
     * @return 状态描述
     */
    @Schema(description = "执行状态描述", example = "成功")
    public String getExecutionStatusDescription() {
        if (executionStatus == null) {
            return "未知";
        }
        switch (executionStatus) {
            case 0:
                return "失败";
            case 1:
                return "成功";
            case 2:
                return "处理中";
            default:
                return "未知";
        }
    }

    /**
     * 判断是否执行成功
     * 
     * @return 是否成功
     */
    @Schema(description = "是否执行成功", example = "true")
    public boolean isSuccess() {
        return Integer.valueOf(1).equals(executionStatus);
    }

    /**
     * 判断是否执行失败
     * 
     * @return 是否失败
     */
    @Schema(description = "是否执行失败", example = "false")
    public boolean isFailure() {
        return Integer.valueOf(0).equals(executionStatus);
    }

    /**
     * 判断是否正在处理中
     * 
     * @return 是否处理中
     */
    @Schema(description = "是否正在处理中", example = "false")
    public boolean isProcessing() {
        return Integer.valueOf(2).equals(executionStatus);
    }

    @Override
    public String toString() {
        return "TriggerExecutionLogDto{" +
                "id=" + id +
                ", triggerId='" + triggerId + '\'' +
                ", workflowInstanceId='" + workflowInstanceId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", executionStatus=" + executionStatus +
                ", executionTime=" + executionTime +
                ", durationMs=" + durationMs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TriggerExecutionLogDto that = (TriggerExecutionLogDto) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
