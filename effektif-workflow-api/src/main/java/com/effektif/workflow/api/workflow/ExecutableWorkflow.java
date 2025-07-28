/*
 * Copyright 2014 Effektif GmbH.
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
package com.effektif.workflow.api.workflow;

import org.joda.time.LocalDateTime;

import com.effektif.workflow.api.WorkflowEngine;
import com.effektif.workflow.api.bpmn.BpmnReader;
import com.effektif.workflow.api.bpmn.BpmnWriter;
import com.effektif.workflow.api.bpmn.XmlElement;
import com.effektif.workflow.api.model.WorkflowId;
import com.effektif.workflow.api.types.DataType;


/**
 * 可执行工作流定义类 - 用于部署到工作流引擎的工作流定义
 *
 * 这是工作流定义的API表示形式，采用建造者模式(Builder Pattern)设计，
 * 支持链式调用来构建复杂的工作流定义。
 *
 * 核心特性：
 * 1. 多格式支持：可以转换为JSON、BPMN XML、数据库格式等
 * 2. 链式构建：支持流畅的API来定义活动、流转、变量等
 * 3. 版本管理：通过sourceWorkflowId支持工作流版本控制
 * 4. 元数据管理：包含创建时间、创建者等元信息
 *
 * 继承层次：
 * ExecutableWorkflow → AbstractWorkflow → Scope → Element
 *
 * 使用示例：
 * <pre>{@code
 * ExecutableWorkflow workflow = new ExecutableWorkflow()
 *   .sourceWorkflowId("vacation-request")
 *   .name("请假申请流程")
 *   .variable("applicant", new TextType())
 *   .variable("days", new NumberType())
 *   .activity("start", new StartEvent()
 *     .transitionTo("apply"))
 *   .activity("apply", new ReceiveTask()
 *     .name("提交申请")
 *     .transitionTo("approve"))
 *   .activity("approve", new ReceiveTask()
 *     .name("经理审批")
 *     .transitionTo("end"))
 *   .activity("end", new EndEvent());
 * }</pre>
 *
 * 支持的序列化格式：
 * <ul>
 *   <li>JSON Java格式 (Map和List)</li>
 *   <li>JSON字符串格式</li>
 *   <li>BPMN 2.0 XML格式</li>
 *   <li>数据库存储格式</li>
 * </ul>
 *
 * BPMN XML示例：
 * <pre>{@code
 * <process id="vacationRequest" name="Vacation request">
 *   <startEvent id="start"/>
 *   <userTask id="apply" name="Submit application"/>
 *   <userTask id="approve" name="Manager approval"/>
 *   <endEvent id="end"/>
 *   <sequenceFlow sourceRef="start" targetRef="apply"/>
 *   <sequenceFlow sourceRef="apply" targetRef="approve"/>
 *   <sequenceFlow sourceRef="approve" targetRef="end"/>
 * </process>
 * }</pre>
 *
 * @see WorkflowEngine#deployWorkflow(ExecutableWorkflow) 部署工作流
 * @see AbstractWorkflow 工作流基类
 * @see Activity 活动定义
 * @see Transition 流转定义
 * @author Tom Baeyens
 */
public class ExecutableWorkflow extends AbstractWorkflow {

  /**
   * 源工作流ID - 用于版本控制和工作流族管理
   *
   * 同一个sourceWorkflowId可以对应多个不同版本的工作流定义，
   * 系统可以根据这个ID查找最新版本或特定版本的工作流。
   *
   * @see #sourceWorkflowId(String)
   */
  protected String sourceWorkflowId;

  /** 工作流创建时间 */
  protected LocalDateTime createTime;

  /** 工作流创建者ID */
  protected String creatorId;

  @Override
  public void readBpmn(BpmnReader r) {
    r.startExtensionElements();
    name = r.readStringAttributeBpmn("name");
    sourceWorkflowId = r.readStringValue("sourceWorkflowId");
    createTime = r.readDateValue("createTime");
    creatorId = r.readStringValue("creatorId");
    enableCases = Boolean.valueOf(r.readStringValue("enableCases"));

    // TODO move access control in a property?
//    for (XmlElement nestedElemenet : r.readElementsEffektif("access")) {
//      r.startElement(nestedElemenet);
//      access = new AccessControlList();
//      access.readBpmn(r);
//      r.endElement();
//    }
    for (XmlElement nestedElement: r.readElementsEffektif("variable")) {
      r.startElement(nestedElement);
      Variable variable = new Variable();
      variable.readBpmn(r);
      variable(variable);
      r.endElement();
    }
    for (XmlElement nestedElement: r.readElementsEffektif("trigger")) {
      r.startElement(nestedElement);
      trigger = r.readTriggerEffektif();
      r.endElement();
    }

    r.endExtensionElements();
    super.readBpmn(r);
  }

  /**
   * Writes workflow-level BPMN, implemented here instead of partly in {@link AbstractWorkflow} (for superclass fields)
   * because there can only be one <code>process/extensionElements</code> element in the output.
   */
  @Override
  public void writeBpmn(BpmnWriter w) {
    super.writeBpmn(w);
    w.startExtensionElements();
    w.writeStringValue("sourceWorkflowId", "value", sourceWorkflowId);
    w.writeStringValue("creatorId", "value", creatorId);
    w.writeStringValue("enableCases", "value", enableCases);

    if (createTime != null) {
      w.startElementEffektif("createTime");
      w.writeDateAttributeEffektif("value", createTime);
      w.endElement();
    }

// TODO move access control in a property?
//    if (access != null) {
//      access.writeBpmn(w);
//    }
    if (variables != null) {
      for (Variable variable : variables) {
        variable.writeBpmn(w);
      }
    }
    if (trigger != null) {
      trigger.writeBpmn(w);
    }

    w.endExtensionElements();
  }

  /** refers to the id in the source (or authoring) form of this workflow.
   * @see #sourceWorkflowId(String) */
  public String getSourceWorkflowId() {
    return this.sourceWorkflowId;
  }
  
  /** refers to the id in the source (or authoring) form of this workflow.
   * @see #sourceWorkflowId(String) */
  public void setSourceWorkflowId(String source) {
    this.sourceWorkflowId = source;
  }

  /** refers to the id in the source (or authoring) form of this workflow.
   * Authoring and source mean that there is some file or form that gets edited 
   * and changes.  Then a snapshot of this sourceform gets deployed as an executable workflow.
   * Workflows are authored in some editor.  Either a file editor or the Effektif product editor.
   * The 3 sources where a workflow could be created are:
   * 1) The Java API.  You could deploy a workflow with the same value {@link #sourceWorkflowId(String)} 
   * multiple times.
   * 2) A BPMN-file.  When parsing a BPMN-file, then the sourceWorkflowId is set to the 
   * id attribute of the process element.
   * 3) The Effektif product editor.  In that case, the workflows you see in the tool 
   * are in fact editor workflows.  You'll find the editor workflow id as the sourceWorkflowId 
   * in the deployed processes.*/
  public ExecutableWorkflow sourceWorkflowId(String sourceWorkflowId) {
    this.sourceWorkflowId = sourceWorkflowId;
    return this;
  }
  
  public LocalDateTime getCreateTime() {
    return this.createTime;
  }
  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }
  public ExecutableWorkflow createTime(LocalDateTime createTime) {
    this.createTime = createTime;
    return this;
  }
  
  public String getCreatorId() {
    return this.creatorId;
  }
  public void setCreatorId(String id) {
    this.creatorId = id;
  }
  public ExecutableWorkflow creatorId(String id) {
    this.creatorId = id;
    return this;
  }

  /** add an activity to the workflow */
  @Override
  public ExecutableWorkflow activity(Activity activity) {
    super.activity(activity);
    return this;
  }
  /** add an activity to the workflow */
  @Override
  public ExecutableWorkflow activity(String id, Activity activity) {
    super.activity(id, activity);
    return this;
  }
  /** add a transition to this workflow where the id is specified in the transition */
  @Override
  public ExecutableWorkflow transition(Transition transition) {
    super.transition(transition);
    return this;
  }
  /** add a transition to this workflow and set the given id. */
  @Override
  public ExecutableWorkflow transition(String id, Transition transition) {
    super.transition(id, transition);
    return this;
  }
  /** add a variable to this workflow and set the given id. */
  @Override
  public ExecutableWorkflow variable(String id, DataType type) {
    super.variable(id, type);
    return this;
  }
  /** add a timer to this workflow. */
  @Override
  public ExecutableWorkflow timer(Timer timer) {
    super.timer(timer);
    return this;
  }
  /** sets the id of this workflow.
   * The id is not really used during execution. */
  @Override
  public ExecutableWorkflow id(WorkflowId id) {
    super.id(id);
    return this;
  }
  
  @Override
  public ExecutableWorkflow name(String name) {
    super.name(name);
    return this;
  }

  @Override
  public ExecutableWorkflow description(String description) {
    super.description(description);
    return this;
  }

  @Override
  public ExecutableWorkflow property(String key, Object value) {
    super.property(key, value);
    return this;
  }
  
  @Override
  public ExecutableWorkflow propertyOpt(String key, Object value) {
    super.propertyOpt(key, value);
    return this;
  }

  @Override
  public ExecutableWorkflow trigger(Trigger trigger) {
    super.trigger(trigger);
    return this;
  }

  @Override
  public ExecutableWorkflow variable(Variable variable) {
    super.variable(variable);
    return this;
  }
}
