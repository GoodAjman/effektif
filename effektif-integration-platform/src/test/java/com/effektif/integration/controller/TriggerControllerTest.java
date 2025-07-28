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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.effektif.integration.model.TriggerConfigDto;
import com.effektif.integration.model.TriggerExecutionLogDto;
import com.effektif.integration.service.TriggerService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 触发器控制器单元测试
 * 
 * @author Integration Platform Team
 */
@WebMvcTest(TriggerController.class)
public class TriggerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TriggerService triggerService;

    @Autowired
    private ObjectMapper objectMapper;

    private TriggerConfigDto sampleTriggerConfig;
    private TriggerExecutionLogDto sampleExecutionLog;

    @BeforeEach
    void setUp() {
        // 创建示例触发器配置
        sampleTriggerConfig = new TriggerConfigDto();
        sampleTriggerConfig.setTriggerId("test-trigger-001");
        sampleTriggerConfig.setTriggerType("httpTrigger");
        sampleTriggerConfig.setWorkflowId("test-workflow-001");
        sampleTriggerConfig.setName("测试HTTP触发器");
        sampleTriggerConfig.setDescription("用于测试的HTTP触发器");
        sampleTriggerConfig.setStatus(1);
        sampleTriggerConfig.setCreatedTime(LocalDateTime.now());
        sampleTriggerConfig.setUpdatedTime(LocalDateTime.now());
        sampleTriggerConfig.setCreatedBy("test-user");
        sampleTriggerConfig.setUpdatedBy("test-user");

        Map<String, Object> config = new HashMap<>();
        config.put("url", "/webhooks/test");
        config.put("method", "POST");
        config.put("secretKey", "test-secret");
        sampleTriggerConfig.setConfig(config);

        // 创建示例执行日志
        sampleExecutionLog = new TriggerExecutionLogDto();
        sampleExecutionLog.setId(1L);
        sampleExecutionLog.setTriggerId("test-trigger-001");
        sampleExecutionLog.setWorkflowInstanceId("wf-instance-001");
        sampleExecutionLog.setExecutionId("exec-001");
        sampleExecutionLog.setExecutionStatus(1);
        sampleExecutionLog.setExecutionTime(LocalDateTime.now());
        sampleExecutionLog.setDurationMs(1500L);

        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("orderId", "12345");
        triggerData.put("amount", 100.00);
        sampleExecutionLog.setTriggerData(triggerData);
    }

    @Test
    void testCreateTrigger() throws Exception {
        when(triggerService.createTrigger(any(TriggerConfigDto.class))).thenReturn(sampleTriggerConfig);

        mockMvc.perform(post("/api/triggers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTriggerConfig)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.triggerId").value("test-trigger-001"))
                .andExpect(jsonPath("$.triggerType").value("httpTrigger"))
                .andExpect(jsonPath("$.workflowId").value("test-workflow-001"))
                .andExpect(jsonPath("$.name").value("测试HTTP触发器"))
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    void testCreateTriggerWithInvalidData() throws Exception {
        TriggerConfigDto invalidConfig = new TriggerConfigDto();
        // 缺少必要字段

        mockMvc.perform(post("/api/triggers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidConfig)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateTrigger() throws Exception {
        when(triggerService.updateTrigger(any(TriggerConfigDto.class))).thenReturn(sampleTriggerConfig);

        mockMvc.perform(put("/api/triggers/test-trigger-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTriggerConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggerId").value("test-trigger-001"))
                .andExpect(jsonPath("$.name").value("测试HTTP触发器"));
    }

    @Test
    void testGetTrigger() throws Exception {
        when(triggerService.getTrigger("test-trigger-001")).thenReturn(sampleTriggerConfig);

        mockMvc.perform(get("/api/triggers/test-trigger-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggerId").value("test-trigger-001"))
                .andExpect(jsonPath("$.triggerType").value("httpTrigger"));
    }

    @Test
    void testGetTriggerNotFound() throws Exception {
        when(triggerService.getTrigger("non-existent")).thenReturn(null);

        mockMvc.perform(get("/api/triggers/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTriggers() throws Exception {
        List<TriggerConfigDto> triggers = Arrays.asList(sampleTriggerConfig);
        when(triggerService.getTriggers(anyString(), any(), anyInt(), anyInt())).thenReturn(triggers);

        mockMvc.perform(get("/api/triggers")
                .param("triggerType", "httpTrigger")
                .param("status", "1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].triggerId").value("test-trigger-001"));
    }

    @Test
    void testDeleteTrigger() throws Exception {
        when(triggerService.deleteTrigger("test-trigger-001")).thenReturn(true);

        mockMvc.perform(delete("/api/triggers/test-trigger-001"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteTriggerNotFound() throws Exception {
        when(triggerService.deleteTrigger("non-existent")).thenReturn(false);

        mockMvc.perform(delete("/api/triggers/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateTriggerStatus() throws Exception {
        when(triggerService.updateTriggerStatus("test-trigger-001", true)).thenReturn(true);

        mockMvc.perform(put("/api/triggers/test-trigger-001/status")
                .param("enabled", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void testHandleHttpTrigger() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("executionId", "exec-001");
        result.put("workflowInstanceId", "wf-instance-001");
        result.put("message", "Workflow started successfully");

        when(triggerService.processHttpTrigger(anyString(), any())).thenReturn(result);

        mockMvc.perform(post("/api/triggers/http/test-trigger-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": \"12345\", \"amount\": 100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.executionId").value("exec-001"))
                .andExpect(jsonPath("$.workflowInstanceId").value("wf-instance-001"));
    }

    @Test
    void testHandleHttpTriggerWithAuthenticationFailure() throws Exception {
        when(triggerService.processHttpTrigger(anyString(), any()))
                .thenThrow(new SecurityException("Authentication failed"));

        mockMvc.perform(post("/api/triggers/http/test-trigger-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": \"12345\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetTriggerExecutionLogs() throws Exception {
        List<TriggerExecutionLogDto> logs = Arrays.asList(sampleExecutionLog);
        when(triggerService.getTriggerExecutionLogs("test-trigger-001", 0, 20)).thenReturn(logs);

        mockMvc.perform(get("/api/triggers/test-trigger-001/logs")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].triggerId").value("test-trigger-001"))
                .andExpect(jsonPath("$[0].executionStatus").value(1))
                .andExpect(jsonPath("$[0].durationMs").value(1500));
    }

    @Test
    void testExecuteTrigger() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("executionId", "exec-002");
        result.put("workflowInstanceId", "wf-instance-002");
        result.put("message", "Workflow started successfully");

        Map<String, Object> data = new HashMap<>();
        data.put("testData", "test-value");

        when(triggerService.executeTrigger("test-trigger-001", data)).thenReturn(result);

        mockMvc.perform(post("/api/triggers/test-trigger-001/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.executionId").value("exec-002"));
    }

    @Test
    void testExecuteTriggerWithoutData() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("executionId", "exec-003");
        result.put("workflowInstanceId", "wf-instance-003");
        result.put("message", "Workflow started successfully");

        when(triggerService.executeTrigger("test-trigger-001", null)).thenReturn(result);

        mockMvc.perform(post("/api/triggers/test-trigger-001/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testHandleInternalServerError() throws Exception {
        when(triggerService.getTrigger("test-trigger-001"))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/triggers/test-trigger-001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testContentTypeValidation() throws Exception {
        mockMvc.perform(post("/api/triggers")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void testParameterValidation() throws Exception {
        mockMvc.perform(get("/api/triggers")
                .param("page", "-1")
                .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
