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
 * HTTP触发器配置类
 * 
 * 用于配置HTTP Webhook触发器的各种参数，包括：
 * - URL路径和HTTP方法
 * - 请求头和内容类型
 * - 签名验证和安全控制
 * - 异步处理配置
 * 
 * @author Integration Platform Team
 */
public class HttpTrigger {

    /** HTTP触发器的URL路径 */
    private String url;
    
    /** HTTP方法，默认为POST */
    private String method = "POST";
    
    /** 请求头配置 */
    private Map<String, String> headers;
    
    /** 内容类型，默认为application/json */
    private String contentType = "application/json";
    
    /** 用于签名验证的密钥 */
    private String secretKey;
    
    /** 是否异步处理，默认为true */
    private boolean async = true;
    
    /** IP白名单，为空表示不限制 */
    private String[] allowedIps;
    
    /** 超时时间（秒），默认30秒 */
    private int timeoutSeconds = 30;
    
    /** 是否启用签名验证，默认为true */
    private boolean enableSignatureVerification = true;
    
    /** 签名算法，默认为HMAC-SHA256 */
    private String signatureAlgorithm = "HmacSHA256";
    
    /** 签名头名称，默认为X-Signature */
    private String signatureHeader = "X-Signature";

    /** 输出变量映射 */
    private Map<String, String> outputs;

    // 默认构造函数
    public HttpTrigger() {
    }

    // Getter and Setter methods
    public String getUrl() {
        return url;
    }

    public HttpTrigger url(String url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public HttpTrigger method(String method) {
        this.method = method;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HttpTrigger headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HttpTrigger header(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }

    public String getContentType() {
        return contentType;
    }

    public HttpTrigger contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public HttpTrigger secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public HttpTrigger async(boolean async) {
        this.async = async;
        return this;
    }

    public String[] getAllowedIps() {
        return allowedIps;
    }

    public HttpTrigger allowedIps(String... allowedIps) {
        this.allowedIps = allowedIps;
        return this;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public HttpTrigger timeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    public boolean isEnableSignatureVerification() {
        return enableSignatureVerification;
    }

    public HttpTrigger enableSignatureVerification(boolean enableSignatureVerification) {
        this.enableSignatureVerification = enableSignatureVerification;
        return this;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public HttpTrigger signatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
        return this;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }

    public HttpTrigger signatureHeader(String signatureHeader) {
        this.signatureHeader = signatureHeader;
        return this;
    }

    public Map<String, String> getOutputs() {
        return outputs;
    }

    public HttpTrigger outputs(Map<String, String> outputs) {
        this.outputs = outputs;
        return this;
    }

    public HttpTrigger output(String key, String outputVariableId) {
        if (outputs == null) {
            outputs = new HashMap<>();
        }
        outputs.put(key, outputVariableId);
        return this;
    }

    @Override
    public String toString() {
        return "HttpTrigger{" +
                "url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", contentType='" + contentType + '\'' +
                ", async=" + async +
                ", enableSignatureVerification=" + enableSignatureVerification +
                '}';
    }
}
