

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
 * 用户持券关系
 * <p>
 * 对应 t_user_coupon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_coupon")
public class UserCouponDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 平台用户 ID
     */
    private String userId;

    /**
     * 券码
     */
    private String couponCode;

    /**
     * 未使用、已使用或已过期
     */
    private String status;

    /**
     * 核销到哪一单，取消订单时按它退回
     */
    private String orderNo;

    /**
     * 领取时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Timestamp createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp updateTime;

    /**
     * 核销时间
     */
    private Timestamp useTime;
}
