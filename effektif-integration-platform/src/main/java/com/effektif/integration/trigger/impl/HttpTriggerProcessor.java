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
package com.effektif.integration.trigger.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.effektif.integration.trigger.HttpTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP触发器处理器
 * 
 * 负责处理HTTP请求触发的工作流，包括：
 * - 请求验证（方法、Content-Type、签名、IP白名单）
 * - 请求数据解析（JSON、XML、Form等格式）
 * - 安全控制（签名验证、IP限制）
 * - 异步处理支持
 * 
 * @author Integration Platform Team
 */
@Component
public class HttpTriggerProcessor {
  
    private static final Logger log = LoggerFactory.getLogger(HttpTriggerProcessor.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证HTTP请求的合法性
     * 
     * @param trigger HTTP触发器配置
     * @param request HTTP请求对象
     * @return 验证结果
     */
    public boolean validateRequest(HttpTrigger trigger, HttpServletRequest request) {
        try {
            // 验证HTTP方法
            if (!trigger.getMethod().equalsIgnoreCase(request.getMethod())) {
                log.warn("HTTP method mismatch. Expected: {}, Actual: {}", 
                         trigger.getMethod(), request.getMethod());
                return false;
            }
            
            // 验证Content-Type（仅对有请求体的方法）
            if (hasRequestBody(request.getMethod()) && trigger.getContentType() != null) {
                String contentType = request.getContentType();
                if (contentType == null || !contentType.toLowerCase().contains(trigger.getContentType().toLowerCase())) {
                    log.warn("Content-Type mismatch. Expected: {}, Actual: {}", 
                             trigger.getContentType(), contentType);
                    return false;
                }
            }
            
            // 验证IP白名单
            if (trigger.getAllowedIps() != null && trigger.getAllowedIps().length > 0) {
                String clientIp = getClientIpAddress(request);
                boolean ipAllowed = Arrays.asList(trigger.getAllowedIps()).contains(clientIp);
                if (!ipAllowed) {
                    log.warn("IP address '{}' is not in the allowed list", clientIp);
                    return false;
                }
            }
            
            // 验证签名
            if (trigger.isEnableSignatureVerification()) {
                String signature = request.getHeader(trigger.getSignatureHeader());
                if (signature == null || signature.trim().isEmpty()) {
                    log.warn("Signature header '{}' is missing", trigger.getSignatureHeader());
                    return false;
                }
                
                String payload = getRequestPayload(request);
                if (!verifySignature(trigger, payload, signature)) {
                    log.warn("Signature verification failed");
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating HTTP request", e);
            return false;
        }
    }

    /**
     * 解析HTTP请求数据
     * 
     * @param request HTTP请求对象
     * @return 解析后的数据Map
     */
    public Map<String, Object> parseRequestData(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // 添加请求元数据
            data.put("httpMethod", request.getMethod());
            data.put("requestUrl", request.getRequestURL().toString());
            data.put("queryString", request.getQueryString());
            data.put("remoteAddr", getClientIpAddress(request));
            data.put("userAgent", request.getHeader("User-Agent"));
            data.put("timestamp", System.currentTimeMillis());
            
            // 解析请求头
            Map<String, String> headers = new HashMap<>();
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                headers.put(headerName, request.getHeader(headerName));
            });
            data.put("headers", headers);
            
            // 解析查询参数
            Map<String, String[]> parameterMap = request.getParameterMap();
            if (!parameterMap.isEmpty()) {
                Map<String, Object> parameters = new HashMap<>();
                parameterMap.forEach((key, values) -> {
                    if (values.length == 1) {
                        parameters.put(key, values[0]);
                    } else {
                        parameters.put(key, values);
                    }
                });
                data.put("parameters", parameters);
            }
            
            // 解析请求体
            if (hasRequestBody(request.getMethod())) {
                String payload = getRequestPayload(request);
                if (payload != null && !payload.trim().isEmpty()) {
                    data.put("rawPayload", payload);
                    
                    // 根据Content-Type解析请求体
                    String contentType = request.getContentType();
                    if (contentType != null) {
                        if (contentType.toLowerCase().contains("application/json")) {
                            try {
                                Object jsonData = objectMapper.readValue(payload, Object.class);
                                data.put("jsonPayload", jsonData);
                            } catch (Exception e) {
                                log.warn("Failed to parse JSON payload", e);
                                data.put("parseError", "Invalid JSON format");
                            }
                        } else if (contentType.toLowerCase().contains("application/xml") || 
                                   contentType.toLowerCase().contains("text/xml")) {
                            // XML解析可以在这里实现
                            data.put("xmlPayload", payload);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing HTTP request data", e);
            data.put("parseError", e.getMessage());
        }
        
        return data;
    }

    /**
     * 验证请求签名
     * 
     * @param trigger HTTP触发器配置
     * @param payload 请求载荷
     * @param signature 签名
     * @return 验证结果
     */
    public boolean verifySignature(HttpTrigger trigger, String payload, String signature) {
        try {
            Mac mac = Mac.getInstance(trigger.getSignatureAlgorithm());
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    trigger.getSecretKey().getBytes(StandardCharsets.UTF_8), 
                    trigger.getSignatureAlgorithm());
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(hash);
            
            // 支持多种签名格式
            String actualSignature = signature;
            if (signature.startsWith("sha256=")) {
                actualSignature = signature.substring(7);
            } else if (signature.startsWith("sha1=")) {
                actualSignature = signature.substring(5);
            }
            
            return expectedSignature.equalsIgnoreCase(actualSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    // 辅助方法
    private boolean hasRequestBody(String method) {
        return Arrays.asList("POST", "PUT", "PATCH").contains(method.toUpperCase());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String getRequestPayload(HttpServletRequest request) {
        try {
            StringBuilder payload = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                payload.append(line);
            }
            return payload.toString();
        } catch (IOException e) {
            log.error("Error reading request payload", e);
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
