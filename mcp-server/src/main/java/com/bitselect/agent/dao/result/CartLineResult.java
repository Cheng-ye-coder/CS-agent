

package com.bitselect.agent.mcp.dao.result;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 购物车一行，连同商品的现价、库存与品类
 * <p>
 * 加购价与现价两列都要给：降价了、涨价了、缺货了都得在结算前先说清楚
 */
@Data
public class CartLineResult {

    private Long id;

    private String userId;

    private String skuCode;

    private String skuName;

    private String category;

    private Integer quantity;

    /**
     * 加购价快照，与现价现比得出降价商品
     */
    private BigDecimal addedPrice;

    private BigDecimal price;

    private Integer stock;

    private String status;

    /**
     * 加购时间
     */
    private Timestamp createTime;

    private Timestamp updateTime;
}
