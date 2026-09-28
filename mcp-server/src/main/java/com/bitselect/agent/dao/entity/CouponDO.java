

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

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 券模板
 * <p>
 * 对应 t_coupon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_coupon")
public class CouponDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 券码
     */
    private String couponCode;

    /**
     * 券名
     */
    private String name;

    /**
     * 满减或折扣
     */
    private String type;

    /**
     * 使用门槛金额，0 表示无门槛
     */
    private BigDecimal threshold;

    /**
     * 满减为减免金额，折扣为折扣率
     */
    private BigDecimal discountValue;

    /**
     * 适用品类，为空表示全品类
     */
    private String category;

    /**
     * 生效时间
     */
    private Timestamp validFrom;

    /**
     * 失效时间
     */
    private Timestamp validTo;

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
