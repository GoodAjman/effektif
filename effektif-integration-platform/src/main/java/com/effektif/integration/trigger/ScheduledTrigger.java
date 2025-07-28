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
 * 定时触发器配置类
 * 
 * 用于配置定时触发器的各种参数，包括：
 * - Cron表达式和固定间隔配置
 * - 时区和错过执行策略
 * - 任务持久化和集群配置
 * - 并发控制和超时处理
 * 
 * @author Integration Platform Team
 */
public class ScheduledTrigger {

    /** Cron表达式，用于复杂的时间调度 */
    private String cronExpression;
    
    /** 固定延迟（毫秒），上次执行完成后等待指定时间再执行下次 */
    private long fixedDelay = -1;
    
    /** 固定间隔（毫秒），按固定间隔执行，不考虑上次执行时间 */
    private long fixedRate = -1;
    
    /** 初始延迟（毫秒），首次执行前的等待时间 */
    private long initialDelay = 0;
    
    /** 时区，默认为系统时区 */
    private String timeZone;
    
    /** 是否持久化任务，默认为true */
    private boolean persistent = true;
    
    /** 错过执行策略：DO_NOTHING, FIRE_ONCE_NOW, RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT, RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, RESCHEDULE_NEXT_WITH_REMAINING_COUNT, RESCHEDULE_NEXT_WITH_EXISTING_COUNT */
    private String misfirePolicy = "DO_NOTHING";
    
    /** 任务组名，用于任务分组管理 */
    private String jobGroup = "DEFAULT";
    
    /** 任务名称，如果不指定则使用工作流ID */
    private String jobName;
    
    /** 任务描述 */
    private String jobDescription;
    
    /** 最大执行次数，-1表示无限制 */
    private int maxExecutions = -1;
    
    /** 任务优先级，数值越大优先级越高，默认为5 */
    private int priority = 5;
    
    /** 是否允许并发执行，默认为false */
    private boolean allowConcurrentExecution = false;
    
    /** 执行超时时间（毫秒），默认为0表示不限制 */
    private long executionTimeout = 0;
    
    /** 是否在应用启动时立即执行一次，默认为false */
    private boolean executeOnStartup = false;
    
    /** 任务结束时间，超过此时间后任务将不再执行 */
    private String endTime;
    
    /** 任务开始时间，在此时间之前任务不会执行 */
    private String startTime;

    /** 输出变量映射 */
    private Map<String, String> outputs;

    // 默认构造函数
    public ScheduledTrigger() {
    }

    // Getter and Setter methods with fluent API
    public String getCronExpression() {
        return cronExpression;
    }

    public ScheduledTrigger cronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        return this;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public ScheduledTrigger fixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
        return this;
    }

    public long getFixedRate() {
        return fixedRate;
    }

    public ScheduledTrigger fixedRate(long fixedRate) {
        this.fixedRate = fixedRate;
        return this;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public ScheduledTrigger initialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public ScheduledTrigger timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public ScheduledTrigger persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    public String getMisfirePolicy() {
        return misfirePolicy;
    }

    public ScheduledTrigger misfirePolicy(String misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
        return this;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public ScheduledTrigger jobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
        return this;
    }

    public String getJobName() {
        return jobName;
    }

    public ScheduledTrigger jobName(String jobName) {
        this.jobName = jobName;
        return this;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public ScheduledTrigger jobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
        return this;
    }

    public int getMaxExecutions() {
        return maxExecutions;
    }

    public ScheduledTrigger maxExecutions(int maxExecutions) {
        this.maxExecutions = maxExecutions;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public ScheduledTrigger priority(int priority) {
        this.priority = priority;
        return this;
    }

    public boolean isAllowConcurrentExecution() {
        return allowConcurrentExecution;
    }

    public ScheduledTrigger allowConcurrentExecution(boolean allowConcurrentExecution) {
        this.allowConcurrentExecution = allowConcurrentExecution;
        return this;
    }

    public long getExecutionTimeout() {
        return executionTimeout;
    }

    public ScheduledTrigger executionTimeout(long executionTimeout) {
        this.executionTimeout = executionTimeout;
        return this;
    }

    public boolean isExecuteOnStartup() {
        return executeOnStartup;
    }

    public ScheduledTrigger executeOnStartup(boolean executeOnStartup) {
        this.executeOnStartup = executeOnStartup;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public ScheduledTrigger endTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public ScheduledTrigger startTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public ScheduledTrigger outputs(Map<String, String> outputs) {
        this.outputs = outputs;
        return this;
    }

    public ScheduledTrigger output(String key, String outputVariableId) {
        if (outputs == null) {
            outputs = new HashMap<>();
        }
        outputs.put(key, outputVariableId);
        return this;
    }

    @Override
    public String toString() {
        return "ScheduledTrigger{" +
                "cronExpression='" + cronExpression + '\'' +
                ", fixedDelay=" + fixedDelay +
                ", fixedRate=" + fixedRate +
                ", timeZone='" + timeZone + '\'' +
                ", persistent=" + persistent +
                '}';
    }
}
