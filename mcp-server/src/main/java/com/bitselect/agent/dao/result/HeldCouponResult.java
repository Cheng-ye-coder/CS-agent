

package com.bitselect.agent.mcp.dao.result;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 持券关系与券模板的合并视图
 * <p>
 * heldStatus 只说「有没有被用掉」，过没过期一律按当前时间现判，不落库
 */
@Data
public class HeldCouponResult {

    private String couponCode;

    private String name;

    private String type;

    private BigDecimal threshold;

    private BigDecimal discountValue;

    private String category;

    private Timestamp validFrom;

    private Timestamp validTo;

    private String heldStatus;

    private String orderNo;

    private Timestamp useTime;
}
