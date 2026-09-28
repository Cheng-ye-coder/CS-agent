package com.bitselect.agent.rag.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 【文件用途】查询词映射实体：把用户口语化表达映射成规范术语。
 *
 * 【为什么存在】
 * - 用户说"报销怎么弄"，知识库里写的是"费用报销流程"
 * - 通过映射规则把口语 → 规范术语，提升检索命中率
 *
 * 【数据库表】t_query_term_mapping
 *
 * 【被谁引用】QueryTermMappingMapper、QueryTermMappingAdminService
 */
@Data
@TableName("t_query_term_mapping")
public class QueryTermMappingDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 业务域 / 系统标识，如 biz / group / data_security，可选
     */
    private String domain;

    /**
     * 用户原始短语
     */
    private String sourceTerm;

    /**
     * 归一化后的目标短语
     */
    private String targetTerm;

    /**
     * 匹配类型：1=精确匹配，2=前缀匹配，3=正则匹配，4=整词匹配
     */
    private Integer matchType;

    /**
     * 优先级，数值越小优先级越高（一般长词在前）
     */
    private Integer priority;

    /**
     * 是否生效：1=生效，0=禁用
     */
    private Integer enabled;

    private String remark;

    private String createBy;
    private String updateBy;

    private Date createTime;
    private Date updateTime;
}