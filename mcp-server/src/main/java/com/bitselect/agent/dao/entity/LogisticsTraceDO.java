

package com.bitselect.agent.mcp.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 物流轨迹节点
 * <p>
 * 对应 t_logistics_trace
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_logistics_trace")
public class LogisticsTraceDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 运单号，查询时必须联查订单归属
     */
    private String trackingNo;

    /**
     * 轨迹发生时间，与创建时间不是一回事
     */
    private Timestamp traceTime;

    /**
     * 所在地
     */
    private String location;

    /**
     * 轨迹描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Timestamp createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp updateTime;
}
