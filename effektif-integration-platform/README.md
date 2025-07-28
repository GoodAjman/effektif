# Effektif Integration Platform 2.0

基于Spring Boot的企业级工作流集成平台，提供多种触发器支持和完整的工作流管理功能。

## 🚀 功能特性

### 核心功能
- **多种触发器支持**: HTTP Webhook、消息队列、定时任务、数据库变更、文件监听
- **工作流引擎集成**: 基于Effektif工作流引擎，支持BPMN 2.0标准
- **企业级特性**: 高可用、集群支持、监控告警、安全认证
- **RESTful API**: 完整的REST API接口，支持OpenAPI 3.0文档
- **可视化管理**: 触发器配置管理、执行日志查询、性能监控

### 触发器类型

#### 1. HTTP触发器 (HttpTrigger)
- 支持多种HTTP方法 (GET/POST/PUT/DELETE等)
- 支持多种数据格式 (JSON/XML/Form等)
- 签名验证和IP白名单控制
- 异步处理支持

#### 2. 消息队列触发器 (MessageQueueTrigger)
- 支持RabbitMQ、Kafka、ActiveMQ等
- 消息确认机制和死信队列处理
- 批量消息处理和消息去重
- 重试机制和错误处理

#### 3. 定时触发器 (ScheduledTrigger)
- 支持Cron表达式和固定间隔
- 时区处理和错过执行策略
- 任务持久化和集群部署
- 并发控制和超时处理

#### 4. 数据库触发器 (DatabaseTrigger)
- 支持MySQL、PostgreSQL、Oracle等
- CDC(Change Data Capture)技术
- 表级和行级过滤
- 事务一致性保证

#### 5. 文件触发器 (FileTrigger)
- 文件系统变更监听
- 支持FTP/SFTP远程文件
- 文件过滤规则
- 目录递归监听

## 🏗️ 架构设计

### 系统架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   外部事件源    │    │   Trigger层     │    │  工作流引擎     │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ HTTP请求        │───▶│ HttpTrigger     │───▶│ TriggerInstance │
│ 消息队列        │───▶│ MQTrigger       │───▶│ WorkflowEngine  │
│ 定时任务        │───▶│ ScheduledTrigger│───▶│ WorkflowInstance│
│ 数据库变更      │───▶│ DatabaseTrigger │───▶│                 │
│ 文件变更        │───▶│ FileTrigger     │───▶│                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                ▲
                                │
                    ┌─────────────────┐
                    │ Spring Boot     │
                    │ 集成平台        │
                    ├─────────────────┤
                    │ REST API        │
                    │ 配置管理        │
                    │ 监控告警        │
                    │ 安全认证        │
                    └─────────────────┘
```

### 技术栈
- **框架**: Spring Boot 2.7.x
- **工作流引擎**: Effektif
- **数据库**: MySQL 8.0 / H2 (开发环境)
- **消息队列**: RabbitMQ
- **调度器**: Quartz
- **缓存**: Redis (可选)
- **监控**: Micrometer + Prometheus
- **文档**: OpenAPI 3.0 (Swagger)

## 🚀 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 8.0+ (生产环境)
- RabbitMQ 3.8+ (可选)
- MongoDB 4.0+ (Effektif工作流引擎)

### 1. 克隆项目
```bash
git clone https://github.com/GoodAjman/effektif.git
cd effektif/effektif-integration-platform
```

### 2. 配置数据库
```sql
-- 创建数据库
CREATE DATABASE effektif_integration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'effektif'@'localhost' IDENTIFIED BY 'effektif123';
GRANT ALL PRIVILEGES ON effektif_integration.* TO 'effektif'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 修改配置文件
编辑 `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/effektif_integration
    username: effektif
    password: effektif123
  
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mongodb:
  host: localhost
  port: 27017
  database: effektif
```

### 4. 启动应用
```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=development

# 生产环境
mvn clean package
java -jar target/effektif-integration-platform-2.0.0.jar --spring.profiles.active=production
```

### 5. 访问应用
- **应用地址**: http://localhost:8080/api
- **API文档**: http://localhost:8080/api/swagger-ui.html
- **健康检查**: http://localhost:8080/api/actuator/health
- **指标监控**: http://localhost:8080/api/actuator/metrics

## 📖 使用指南

### 创建HTTP触发器
```bash
curl -X POST http://localhost:8080/api/triggers \
  -H "Content-Type: application/json" \
  -d '{
    "triggerId": "payment-webhook-001",
    "triggerType": "httpTrigger",
    "workflowId": "payment-process-workflow",
    "name": "支付回调触发器",
    "description": "处理第三方支付平台的回调通知",
    "config": {
      "url": "/webhooks/payment",
      "method": "POST",
      "secretKey": "your-secret-key",
      "contentType": "application/json",
      "async": true,
      "enableSignatureVerification": true
    },
    "status": 1
  }'
```

### 触发HTTP Webhook
```bash
curl -X POST http://localhost:8080/api/triggers/http/payment-webhook-001 \
  -H "Content-Type: application/json" \
  -H "X-Signature: sha256=your-signature" \
  -d '{
    "orderId": "ORDER-20240101-001",
    "amount": 299.99,
    "currency": "CNY",
    "status": "success"
  }'
```

### 创建定时触发器
```bash
curl -X POST http://localhost:8080/api/triggers \
  -H "Content-Type: application/json" \
  -d '{
    "triggerId": "daily-sync-001",
    "triggerType": "scheduledTrigger",
    "workflowId": "daily-data-sync-workflow",
    "name": "每日数据同步触发器",
    "config": {
      "cronExpression": "0 0 2 * * ?",
      "timeZone": "Asia/Shanghai",
      "persistent": true,
      "misfirePolicy": "DO_NOTHING"
    },
    "status": 1
  }'
```

### 查询触发器列表
```bash
curl -X GET "http://localhost:8080/api/triggers?triggerType=httpTrigger&status=1&page=0&size=20"
```

### 查看执行日志
```bash
curl -X GET "http://localhost:8080/api/triggers/payment-webhook-001/logs?page=0&size=20"
```

## 🔧 配置说明

### 应用配置
主要配置项说明：

```yaml
effektif:
  integration:
    platform:
      triggers:
        http:
          enabled: true                    # 是否启用HTTP触发器
          base-path: "/webhooks"           # HTTP触发器基础路径
          max-payload-size: 10485760       # 最大请求体大小(10MB)
          request-timeout: 30              # 请求超时时间(秒)
        
        message-queue:
          enabled: true                    # 是否启用消息队列触发器
          default-concurrency: 5           # 默认并发消费者数量
          max-retries: 3                   # 最大重试次数
          retry-interval: 5000             # 重试间隔(毫秒)
        
        scheduled:
          enabled: true                    # 是否启用定时触发器
          thread-pool-size: 10             # 线程池大小
          persistent-enabled: true         # 是否启用任务持久化
          cluster-enabled: true            # 是否启用集群模式
      
      security:
        enabled: true                      # 是否启用安全认证
        jwt-secret: "your-jwt-secret"      # JWT密钥
        token-expiration: 86400            # Token过期时间(秒)
      
      monitoring:
        enabled: true                      # 是否启用监控
        metrics-enabled: true              # 是否启用指标收集
        health-check-enabled: true         # 是否启用健康检查
```

### 环境变量
生产环境可通过环境变量覆盖配置：

```bash
export DATABASE_URL=jdbc:mysql://prod-db:3306/effektif_integration
export DATABASE_USERNAME=effektif_prod
export DATABASE_PASSWORD=your-secure-password
export RABBITMQ_HOST=prod-rabbitmq
export RABBITMQ_USERNAME=effektif
export RABBITMQ_PASSWORD=your-rabbitmq-password
export MONGODB_HOST=prod-mongodb
export MONGODB_DATABASE=effektif_prod
export SERVER_PORT=8080
```

## 🧪 测试

### 运行单元测试
```bash
mvn test
```

### 运行集成测试
```bash
mvn verify -P integration-test
```

### 压力测试
使用JMeter或其他工具进行压力测试：

```bash
# HTTP触发器并发测试
jmeter -n -t tests/http-trigger-load-test.jmx -l results/http-trigger-results.jtl

# 定时触发器稳定性测试
jmeter -n -t tests/scheduled-trigger-stability-test.jmx -l results/scheduled-trigger-results.jtl
```

## 📊 监控和运维

### 健康检查
```bash
curl http://localhost:8080/api/actuator/health
```

### 指标监控
```bash
# 查看所有指标
curl http://localhost:8080/api/actuator/metrics

# 查看特定指标
curl http://localhost:8080/api/actuator/metrics/trigger.execution.count
curl http://localhost:8080/api/actuator/metrics/trigger.execution.duration
```

### Prometheus集成
```bash
# Prometheus指标端点
curl http://localhost:8080/api/actuator/prometheus
```

### 日志管理
日志文件位置：
- 开发环境: `logs/effektif-integration-platform.log`
- 生产环境: `/var/log/effektif/integration-platform.log`

日志级别配置：
```yaml
logging:
  level:
    com.effektif: INFO
    org.springframework.amqp: WARN
```

## 🔒 安全考虑

### 认证和授权
- 支持JWT Token认证
- 基于角色的访问控制(RBAC)
- API密钥管理

### 数据安全
- 敏感配置信息加密存储
- 数据库连接加密
- 审计日志记录

### 网络安全
- HTTPS支持
- IP白名单控制
- 请求签名验证

## 🚀 部署指南

### Docker部署
```dockerfile
FROM openjdk:8-jre-alpine
COPY target/effektif-integration-platform-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# 构建镜像
docker build -t effektif-integration-platform:2.0.0 .

# 运行容器
docker run -d \
  --name effektif-integration-platform \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e DATABASE_URL=jdbc:mysql://mysql:3306/effektif_integration \
  effektif-integration-platform:2.0.0
```

### Kubernetes部署
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: effektif-integration-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: effektif-integration-platform
  template:
    metadata:
      labels:
        app: effektif-integration-platform
    spec:
      containers:
      - name: app
        image: effektif-integration-platform:2.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详情请参阅 [LICENSE](../LICENSE.md) 文件。

## 📞 支持

- 📧 邮箱: support@effektif.com
- 📖 文档: https://docs.effektif.com
- 🐛 问题反馈: https://github.com/GoodAjman/effektif/issues
- 💬 社区讨论: https://community.effektif.com
