-- Effektif Integration Platform 2.0 数据库初始化脚本
-- 创建触发器相关表

-- 创建触发器配置表
CREATE TABLE IF NOT EXISTS trigger_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trigger_id VARCHAR(64) NOT NULL UNIQUE COMMENT '触发器ID',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发器类型',
    workflow_id VARCHAR(64) NOT NULL COMMENT '关联的工作流ID',
    name VARCHAR(128) NOT NULL COMMENT '触发器名称',
    description TEXT COMMENT '描述',
    config_json TEXT NOT NULL COMMENT '触发器配置JSON',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(64) COMMENT '创建人',
    updated_by VARCHAR(64) COMMENT '更新人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器配置表';

-- 创建索引
CREATE INDEX idx_trigger_type ON trigger_config (trigger_type);
CREATE INDEX idx_workflow_id ON trigger_config (workflow_id);
CREATE INDEX idx_status ON trigger_config (status);
CREATE INDEX idx_created_time ON trigger_config (created_time);

-- 创建触发器执行日志表
CREATE TABLE IF NOT EXISTS trigger_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trigger_id VARCHAR(64) NOT NULL COMMENT '触发器ID',
    workflow_instance_id VARCHAR(64) COMMENT '工作流实例ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    trigger_data TEXT COMMENT '触发数据',
    execution_status TINYINT NOT NULL COMMENT '执行状态：0-失败，1-成功，2-处理中',
    error_message TEXT COMMENT '错误信息',
    execution_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器执行日志表';

-- 创建索引
CREATE INDEX idx_trigger_id ON trigger_execution_log (trigger_id);
CREATE INDEX idx_workflow_instance_id ON trigger_execution_log (workflow_instance_id);
CREATE INDEX idx_execution_status ON trigger_execution_log (execution_status);
CREATE INDEX idx_execution_time ON trigger_execution_log (execution_time);
CREATE INDEX idx_execution_id ON trigger_execution_log (execution_id);

-- 插入示例数据
INSERT INTO trigger_config (
    trigger_id, trigger_type, workflow_id, name, description, config_json, status, created_by, updated_by
) VALUES 
(
    'payment-webhook-001', 
    'httpTrigger', 
    'payment-process-workflow', 
    '支付回调触发器', 
    '处理第三方支付平台的回调通知',
    '{"url": "/webhooks/payment", "method": "POST", "secretKey": "payment-secret-key", "contentType": "application/json", "async": true, "enableSignatureVerification": true, "signatureHeader": "X-Signature", "signatureAlgorithm": "HmacSHA256"}',
    1,
    'system',
    'system'
),
(
    'order-queue-001',
    'messageQueueTrigger',
    'order-processing-workflow',
    '订单队列触发器',
    '监听订单创建消息队列',
    '{"queueName": "order.created", "exchangeName": "order.exchange", "routingKey": "order.created", "connectionFactory": "orderConnectionFactory", "concurrency": 5, "durable": true, "autoAck": false, "maxRetries": 3, "retryInterval": 5000}',
    1,
    'system',
    'system'
),
(
    'daily-sync-001',
    'scheduledTrigger',
    'daily-data-sync-workflow',
    '每日数据同步触发器',
    '每天凌晨2点执行数据同步',
    '{"cronExpression": "0 0 2 * * ?", "timeZone": "Asia/Shanghai", "persistent": true, "misfirePolicy": "DO_NOTHING", "jobGroup": "DATA_SYNC", "maxExecutions": -1, "priority": 5}',
    1,
    'system',
    'system'
),
(
    'health-check-001',
    'scheduledTrigger',
    'system-health-check-workflow',
    '系统健康检查触发器',
    '每5分钟检查一次系统状态',
    '{"fixedRate": 300000, "initialDelay": 60000, "persistent": false, "allowConcurrentExecution": false, "executionTimeout": 30000}',
    1,
    'system',
    'system'
);

-- 插入示例执行日志
INSERT INTO trigger_execution_log (
    trigger_id, workflow_instance_id, execution_id, trigger_data, execution_status, execution_time, duration_ms
) VALUES 
(
    'payment-webhook-001',
    'wf-payment-001',
    'exec-payment-001',
    '{"orderId": "ORDER-20240101-001", "amount": 299.99, "currency": "CNY", "paymentMethod": "alipay", "status": "success"}',
    1,
    '2024-01-01 10:30:00',
    1250
),
(
    'payment-webhook-001',
    'wf-payment-002',
    'exec-payment-002',
    '{"orderId": "ORDER-20240101-002", "amount": 599.99, "currency": "CNY", "paymentMethod": "wechat", "status": "success"}',
    1,
    '2024-01-01 11:15:00',
    980
),
(
    'order-queue-001',
    'wf-order-001',
    'exec-order-001',
    '{"orderId": "ORDER-20240101-003", "customerId": "CUST-001", "products": [{"id": "PROD-001", "quantity": 2, "price": 149.99}], "totalAmount": 299.98}',
    1,
    '2024-01-01 12:00:00',
    2100
),
(
    'daily-sync-001',
    'wf-sync-001',
    'exec-sync-001',
    '{"syncDate": "2024-01-01", "triggerTime": "2024-01-01T02:00:00", "scheduledTriggerType": "CRON"}',
    1,
    '2024-01-01 02:00:00',
    45000
),
(
    'health-check-001',
    'wf-health-001',
    'exec-health-001',
    '{"checkTime": "2024-01-01T10:00:00", "executionCount": 1, "scheduledTriggerType": "FIXED_RATE"}',
    1,
    '2024-01-01 10:00:00',
    500
),
(
    'payment-webhook-001',
    NULL,
    'exec-payment-003',
    '{"orderId": "ORDER-20240101-004", "amount": 199.99, "currency": "CNY", "paymentMethod": "alipay"}',
    0,
    '2024-01-01 14:30:00',
    5000
);

-- 创建视图：触发器统计信息
CREATE VIEW trigger_statistics AS
SELECT 
    tc.trigger_id,
    tc.trigger_type,
    tc.name,
    tc.status,
    COUNT(tel.id) as total_executions,
    SUM(CASE WHEN tel.execution_status = 1 THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN tel.execution_status = 0 THEN 1 ELSE 0 END) as failure_count,
    SUM(CASE WHEN tel.execution_status = 2 THEN 1 ELSE 0 END) as processing_count,
    AVG(tel.duration_ms) as avg_duration_ms,
    MAX(tel.duration_ms) as max_duration_ms,
    MIN(tel.duration_ms) as min_duration_ms,
    MAX(tel.execution_time) as last_execution_time
FROM trigger_config tc
LEFT JOIN trigger_execution_log tel ON tc.trigger_id = tel.trigger_id
GROUP BY tc.trigger_id, tc.trigger_type, tc.name, tc.status;

-- 创建存储过程：清理过期日志
DELIMITER //
CREATE PROCEDURE CleanupExpiredLogs(IN days_to_keep INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE trigger_id_var VARCHAR(64);
    DECLARE deleted_count INT DEFAULT 0;
    
    -- 声明游标
    DECLARE trigger_cursor CURSOR FOR 
        SELECT DISTINCT trigger_id FROM trigger_execution_log;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 开始事务
    START TRANSACTION;
    
    -- 打开游标
    OPEN trigger_cursor;
    
    read_loop: LOOP
        FETCH trigger_cursor INTO trigger_id_var;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 删除指定天数之前的日志，但保留每个触发器最近的100条记录
        DELETE FROM trigger_execution_log 
        WHERE trigger_id = trigger_id_var 
        AND execution_time < DATE_SUB(NOW(), INTERVAL days_to_keep DAY)
        AND id NOT IN (
            SELECT id FROM (
                SELECT id FROM trigger_execution_log 
                WHERE trigger_id = trigger_id_var 
                ORDER BY execution_time DESC 
                LIMIT 100
            ) AS recent_logs
        );
        
        SET deleted_count = deleted_count + ROW_COUNT();
    END LOOP;
    
    -- 关闭游标
    CLOSE trigger_cursor;
    
    -- 提交事务
    COMMIT;
    
    -- 返回删除的记录数
    SELECT CONCAT('Deleted ', deleted_count, ' expired log records') AS result;
END //
DELIMITER ;

-- 创建定时清理事件（如果支持事件调度器）
-- SET GLOBAL event_scheduler = ON;
-- 
-- CREATE EVENT IF NOT EXISTS cleanup_trigger_logs
-- ON SCHEDULE EVERY 1 DAY
-- STARTS CURRENT_TIMESTAMP
-- DO
--   CALL CleanupExpiredLogs(30);

-- 创建函数：计算触发器成功率
DELIMITER //
CREATE FUNCTION GetTriggerSuccessRate(trigger_id_param VARCHAR(64))
RETURNS DECIMAL(5,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_count INT DEFAULT 0;
    DECLARE success_count INT DEFAULT 0;
    DECLARE success_rate DECIMAL(5,2) DEFAULT 0.00;
    
    SELECT COUNT(*) INTO total_count
    FROM trigger_execution_log
    WHERE trigger_id = trigger_id_param;
    
    IF total_count > 0 THEN
        SELECT COUNT(*) INTO success_count
        FROM trigger_execution_log
        WHERE trigger_id = trigger_id_param AND execution_status = 1;
        
        SET success_rate = (success_count * 100.0) / total_count;
    END IF;
    
    RETURN success_rate;
END //
DELIMITER ;
