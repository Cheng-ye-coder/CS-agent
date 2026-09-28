

package com.bitselect.agent.mcp.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bitselect.agent.mcp.dao.handler.JsonbStringTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 可下单配置（SKU），下单、加购、扣库存都落在这一层
 * <p>
 * 对应 t_product_sku
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_product_sku", autoResultMap = true)
public class ProductSkuDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 下单编码，取商品的 partNumber
     */
    private String skuCode;

    /**
     * 所属商品款
     */
    private String spuCode;

    /**
     * 含容量颜色的完整商品名
     */
    private String name;

    /**
     * 规格键值 JSON，展示顺序由 BitToolSupport.specs 定
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String specs;

    /**
     * 现价
     */
    private BigDecimal price;

    /**
     * 可售库存
     */
    private Integer stock;

    /**
     * 在售或已下架
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
}
