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
package com.effektif.integration.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.effektif.integration.entity.TriggerExecutionLog;

/**
 * 触发器执行日志Repository接口
 * 
 * 提供触发器执行日志的数据访问操作。
 * 
 * @author Integration Platform Team
 */
@Repository
public interface TriggerExecutionLogRepository extends JpaRepository<TriggerExecutionLog, Long> {

    /**
     * 根据触发器ID查找执行日志
     * 
     * @param triggerId 触发器ID
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByTriggerId(String triggerId, Pageable pageable);

    /**
     * 根据工作流实例ID查找执行日志
     * 
     * @param workflowInstanceId 工作流实例ID
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByWorkflowInstanceId(String workflowInstanceId, Pageable pageable);

    /**
     * 根据执行ID查找执行日志
     * 
     * @param executionId 执行ID
     * @return 执行日志
     */
    TriggerExecutionLog findByExecutionId(String executionId);

    /**
     * 根据执行状态查找执行日志
     * 
     * @param executionStatus 执行状态
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByExecutionStatus(Integer executionStatus, Pageable pageable);

    /**
     * 根据触发器ID和执行状态查找执行日志
     * 
     * @param triggerId 触发器ID
     * @param executionStatus 执行状态
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByTriggerIdAndExecutionStatus(String triggerId, Integer executionStatus, Pageable pageable);

    /**
     * 根据时间范围查找执行日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByExecutionTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据触发器ID和时间范围查找执行日志
     * 
     * @param triggerId 触发器ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    Page<TriggerExecutionLog> findByTriggerIdAndExecutionTimeBetween(String triggerId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查找最近的执行日志
     * 
     * @param triggerId 触发器ID
     * @param limit 限制数量
     * @return 最近的执行日志列表
     */
    @Query("SELECT t FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId ORDER BY t.executionTime DESC")
    List<TriggerExecutionLog> findRecentByTriggerId(@Param("triggerId") String triggerId, Pageable pageable);

    /**
     * 查找失败的执行日志
     * 
     * @param triggerId 触发器ID
     * @param pageable 分页参数
     * @return 失败的执行日志分页列表
     */
    @Query("SELECT t FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.executionStatus = 0 ORDER BY t.executionTime DESC")
    Page<TriggerExecutionLog> findFailedByTriggerId(@Param("triggerId") String triggerId, Pageable pageable);

    /**
     * 统计触发器的执行次数
     * 
     * @param triggerId 触发器ID
     * @return 执行次数
     */
    long countByTriggerId(String triggerId);

    /**
     * 统计触发器的成功执行次数
     * 
     * @param triggerId 触发器ID
     * @return 成功执行次数
     */
    @Query("SELECT COUNT(t) FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.executionStatus = 1")
    long countSuccessByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 统计触发器的失败执行次数
     * 
     * @param triggerId 触发器ID
     * @return 失败执行次数
     */
    @Query("SELECT COUNT(t) FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.executionStatus = 0")
    long countFailureByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 计算触发器的平均执行时间
     * 
     * @param triggerId 触发器ID
     * @return 平均执行时间（毫秒）
     */
    @Query("SELECT AVG(t.durationMs) FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.durationMs IS NOT NULL")
    Double getAverageDurationByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 获取触发器的最大执行时间
     * 
     * @param triggerId 触发器ID
     * @return 最大执行时间（毫秒）
     */
    @Query("SELECT MAX(t.durationMs) FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.durationMs IS NOT NULL")
    Long getMaxDurationByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 获取触发器的最小执行时间
     * 
     * @param triggerId 触发器ID
     * @return 最小执行时间（毫秒）
     */
    @Query("SELECT MIN(t.durationMs) FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.durationMs IS NOT NULL")
    Long getMinDurationByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 根据时间范围统计执行次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行次数
     */
    long countByExecutionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围和状态统计执行次数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param executionStatus 执行状态
     * @return 执行次数
     */
    long countByExecutionTimeBetweenAndExecutionStatus(LocalDateTime startTime, LocalDateTime endTime, Integer executionStatus);

    /**
     * 删除指定触发器的所有执行日志
     * 
     * @param triggerId 触发器ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId")
    int deleteByTriggerId(@Param("triggerId") String triggerId);

    /**
     * 删除指定时间之前的执行日志
     * 
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM TriggerExecutionLog t WHERE t.executionTime < :beforeTime")
    int deleteByExecutionTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除指定触发器在指定时间之前的执行日志
     * 
     * @param triggerId 触发器ID
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM TriggerExecutionLog t WHERE t.triggerId = :triggerId AND t.executionTime < :beforeTime")
    int deleteByTriggerIdAndExecutionTimeBefore(@Param("triggerId") String triggerId, @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查找执行时间超过指定阈值的日志
     * 
     * @param durationThreshold 执行时间阈值（毫秒）
     * @param pageable 分页参数
     * @return 执行时间超过阈值的日志分页列表
     */
    @Query("SELECT t FROM TriggerExecutionLog t WHERE t.durationMs > :durationThreshold ORDER BY t.durationMs DESC")
    Page<TriggerExecutionLog> findByDurationGreaterThan(@Param("durationThreshold") Long durationThreshold, Pageable pageable);

    /**
     * 获取触发器执行统计信息
     * 
     * @param triggerId 触发器ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息数组 [总数, 成功数, 失败数, 处理中数]
     */
    @Query("SELECT " +
           "COUNT(t), " +
           "SUM(CASE WHEN t.executionStatus = 1 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.executionStatus = 0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.executionStatus = 2 THEN 1 ELSE 0 END) " +
           "FROM TriggerExecutionLog t " +
           "WHERE t.triggerId = :triggerId " +
           "AND t.executionTime BETWEEN :startTime AND :endTime")
    Object[] getExecutionStatistics(@Param("triggerId") String triggerId, 
                                   @Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 复合查询：根据多个条件查找执行日志
     * 
     * @param triggerId 触发器ID（可选）
     * @param executionStatus 执行状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageable 分页参数
     * @return 执行日志分页列表
     */
    @Query("SELECT t FROM TriggerExecutionLog t WHERE " +
           "(:triggerId IS NULL OR t.triggerId = :triggerId) AND " +
           "(:executionStatus IS NULL OR t.executionStatus = :executionStatus) AND " +
           "(:startTime IS NULL OR t.executionTime >= :startTime) AND " +
           "(:endTime IS NULL OR t.executionTime <= :endTime) " +
           "ORDER BY t.executionTime DESC")
    Page<TriggerExecutionLog> findByMultipleConditions(@Param("triggerId") String triggerId,
                                                      @Param("executionStatus") Integer executionStatus,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime,
                                                      Pageable pageable);
}
