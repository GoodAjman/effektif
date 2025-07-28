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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.effektif.integration.entity.TriggerConfig;

/**
 * 触发器配置Repository接口
 * 
 * 提供触发器配置的数据访问操作。
 * 
 * @author Integration Platform Team
 */
@Repository
public interface TriggerConfigRepository extends JpaRepository<TriggerConfig, Long> {

    /**
     * 根据触发器ID查找触发器配置
     * 
     * @param triggerId 触发器ID
     * @return 触发器配置
     */
    TriggerConfig findByTriggerId(String triggerId);

    /**
     * 检查触发器ID是否存在
     * 
     * @param triggerId 触发器ID
     * @return 是否存在
     */
    boolean existsByTriggerId(String triggerId);

    /**
     * 根据触发器类型查找触发器配置列表
     * 
     * @param triggerType 触发器类型
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    Page<TriggerConfig> findByTriggerType(String triggerType, Pageable pageable);

    /**
     * 根据状态查找触发器配置列表
     * 
     * @param status 状态
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    Page<TriggerConfig> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据触发器类型和状态查找触发器配置列表
     * 
     * @param triggerType 触发器类型
     * @param status 状态
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    Page<TriggerConfig> findByTriggerTypeAndStatus(String triggerType, Integer status, Pageable pageable);

    /**
     * 根据工作流ID查找触发器配置列表
     * 
     * @param workflowId 工作流ID
     * @return 触发器配置列表
     */
    List<TriggerConfig> findByWorkflowId(String workflowId);

    /**
     * 根据工作流ID查找启用的触发器配置列表
     * 
     * @param workflowId 工作流ID
     * @return 启用的触发器配置列表
     */
    List<TriggerConfig> findByWorkflowIdAndStatus(String workflowId, Integer status);

    /**
     * 根据创建人查找触发器配置列表
     * 
     * @param createdBy 创建人
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    Page<TriggerConfig> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * 根据名称模糊查找触发器配置列表
     * 
     * @param name 名称关键字
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    Page<TriggerConfig> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 查找所有启用的触发器配置
     * 
     * @return 启用的触发器配置列表
     */
    @Query("SELECT t FROM TriggerConfig t WHERE t.status = 1 ORDER BY t.createdTime DESC")
    List<TriggerConfig> findAllEnabled();

    /**
     * 根据触发器类型查找所有启用的触发器配置
     * 
     * @param triggerType 触发器类型
     * @return 启用的触发器配置列表
     */
    @Query("SELECT t FROM TriggerConfig t WHERE t.triggerType = :triggerType AND t.status = 1 ORDER BY t.createdTime DESC")
    List<TriggerConfig> findEnabledByTriggerType(@Param("triggerType") String triggerType);

    /**
     * 统计触发器配置总数
     * 
     * @return 总数
     */
    @Query("SELECT COUNT(t) FROM TriggerConfig t")
    long countAll();

    /**
     * 根据状态统计触发器配置数量
     * 
     * @param status 状态
     * @return 数量
     */
    long countByStatus(Integer status);

    /**
     * 根据触发器类型统计触发器配置数量
     * 
     * @param triggerType 触发器类型
     * @return 数量
     */
    long countByTriggerType(String triggerType);

    /**
     * 根据工作流ID统计触发器配置数量
     * 
     * @param workflowId 工作流ID
     * @return 数量
     */
    long countByWorkflowId(String workflowId);

    /**
     * 批量更新触发器状态
     * 
     * @param triggerIds 触发器ID列表
     * @param status 新状态
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE TriggerConfig t SET t.status = :status, t.updatedBy = :updatedBy, t.updatedTime = CURRENT_TIMESTAMP WHERE t.triggerId IN :triggerIds")
    int updateStatusByTriggerIds(@Param("triggerIds") List<String> triggerIds, 
                                @Param("status") Integer status, 
                                @Param("updatedBy") String updatedBy);

    /**
     * 根据工作流ID批量更新触发器状态
     * 
     * @param workflowId 工作流ID
     * @param status 新状态
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE TriggerConfig t SET t.status = :status, t.updatedBy = :updatedBy, t.updatedTime = CURRENT_TIMESTAMP WHERE t.workflowId = :workflowId")
    int updateStatusByWorkflowId(@Param("workflowId") String workflowId, 
                                @Param("status") Integer status, 
                                @Param("updatedBy") String updatedBy);

    /**
     * 删除指定工作流ID的所有触发器配置
     * 
     * @param workflowId 工作流ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM TriggerConfig t WHERE t.workflowId = :workflowId")
    int deleteByWorkflowId(@Param("workflowId") String workflowId);

    /**
     * 查找需要清理的过期触发器配置（示例：30天前创建且已禁用的配置）
     * 
     * @return 过期的触发器配置列表
     */
    @Query("SELECT t FROM TriggerConfig t WHERE t.status = 0 AND t.createdTime < CURRENT_TIMESTAMP - 30 ORDER BY t.createdTime ASC")
    List<TriggerConfig> findExpiredTriggers();

    /**
     * 复合查询：根据多个条件查找触发器配置
     * 
     * @param triggerType 触发器类型（可选）
     * @param status 状态（可选）
     * @param workflowId 工作流ID（可选）
     * @param createdBy 创建人（可选）
     * @param pageable 分页参数
     * @return 触发器配置分页列表
     */
    @Query("SELECT t FROM TriggerConfig t WHERE " +
           "(:triggerType IS NULL OR t.triggerType = :triggerType) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:workflowId IS NULL OR t.workflowId = :workflowId) AND " +
           "(:createdBy IS NULL OR t.createdBy = :createdBy) " +
           "ORDER BY t.createdTime DESC")
    Page<TriggerConfig> findByMultipleConditions(@Param("triggerType") String triggerType,
                                                @Param("status") Integer status,
                                                @Param("workflowId") String workflowId,
                                                @Param("createdBy") String createdBy,
                                                Pageable pageable);
}
