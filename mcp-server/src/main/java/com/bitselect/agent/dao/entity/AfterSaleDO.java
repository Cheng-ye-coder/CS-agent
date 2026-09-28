

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
 * 售后单，不回写订单状态
 * <p>
 * 对应 t_after_sale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_after_sale")
public class AfterSaleDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 售后单号
     */
    private String afterSaleNo;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 下单编码
     */
    private String skuCode;

    /**
     * 退货退款、换货或仅退款
     */
    private String type;

    /**
     * 申请原因，取自用户明确说明
     */
    private String reason;

    /**
     * 售后状态
     */
    private String status;

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

    /**
     * 完成时间
     */
    private Timestamp finishTime;
}
