

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
 * 购物车
 * <p>
 * 对应 t_cart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_cart")
public class CartDO {

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
     * 下单编码
     */
    private String skuCode;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 加购价快照，与现价现比得出降价商品
     */
    private BigDecimal addedPrice;

    /**
     * 加购时间，设置数量时不刷新
     */
    @TableField(fill = FieldFill.INSERT)
    private Timestamp createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp updateTime;
}
