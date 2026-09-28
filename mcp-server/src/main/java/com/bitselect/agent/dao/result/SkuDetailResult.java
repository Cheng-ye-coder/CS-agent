

package com.bitselect.agent.mcp.dao.result;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 可下单配置连同它所属款的展示字段，省掉执行器再查一次款
 */
@Data
public class SkuDetailResult {

    private String skuCode;

    private String spuCode;

    private String spuName;

    private String name;

    private String category;

    private String subCategory;

    /**
     * 规格键值 JSON 原文，展示顺序由 BitToolSupport.specs 定
     */
    private String specs;

    /**
     * 所属款的公共属性 JSON 原文，配置本身不重复存
     */
    private String spuSpecs;

    private BigDecimal price;

    private Integer stock;

    private String status;
}
