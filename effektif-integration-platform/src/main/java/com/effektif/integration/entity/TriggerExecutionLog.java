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
package com.effektif.integration.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 触发器执行日志实体类
 * 
 * 对应数据库表 trigger_execution_log，记录触发器的执行历史。
 * 
 * @author Integration Platform Team
 */
@Entity
@Table(name = "trigger_execution_log", indexes = {
    @Index(name = "idx_trigger_id", columnList = "trigger_id"),
    @Index(name = "idx_workflow_instance_id", columnList = "workflow_instance_id"),
    @Index(name = "idx_execution_status", columnList = "execution_status"),
    @Index(name = "idx_execution_time", columnList = "execution_time")
})
public class TriggerExecutionLog {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 触发器ID
     */
    @Column(name = "trigger_id", nullable = false, length = 64)
    private String triggerId;

    /**
     * 工作流实例ID
     */
    @Column(name = "workflow_instance_id", length = 64)
    private String workflowInstanceId;

    /**
     * 执行ID，用于追踪单次执行
     */
    @Column(name = "execution_id", nullable = false, length = 64)
    private String executionId;

    /**
     * 触发数据JSON
     */
    @Column(name = "trigger_data", columnDefinition = "TEXT")
    private String triggerData;

    /**
     * 执行状态：0-失败，1-成功，2-处理中
     */
    @Column(name = "execution_status", nullable = false)
    private Integer executionStatus;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行时间
     */
    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    // 默认构造函数
    public TriggerExecutionLog() {
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

    public String getTriggerData() {
        return triggerData;
    }

    public void setTriggerData(String triggerData) {
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

    @Override
    public String toString() {
        return "TriggerExecutionLog{" +
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

        TriggerExecutionLog that = (TriggerExecutionLog) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
