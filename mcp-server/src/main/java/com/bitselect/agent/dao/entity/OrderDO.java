

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
 * 订单头
 * <p>
 * 对应 t_order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order")
public class OrderDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 平台用户 ID，业务库不设跨库外键
     */
    private String userId;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 商品总额
     */
    private BigDecimal totalAmount;

    /**
     * 优惠额
     */
    private BigDecimal discountAmount;

    /**
     * 实付额
     */
    private BigDecimal payAmount;

    /**
     * 使用的券码
     */
    private String couponCode;

    /**
     * 收货人
     */
    private String receiverName;

    /**
     * 收货手机号，查询工具返回前打码
     */
    private String receiverPhone;

    /**
     * 收货地址，查询工具返回时只到区级
     */
    private String receiverAddress;

    /**
     * 运单号
     */
    private String trackingNo;

    /**
     * 下单时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Timestamp createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp updateTime;

    /**
     * 支付时间
     */
    private Timestamp payTime;

    /**
     * 发货时间
     */
    private Timestamp shipTime;

    /**
     * 签收时间，七天无理由窗口按它现算
     */
    private Timestamp receiveTime;

    /**
     * 取消时间
     */
    private Timestamp cancelTime;
}
