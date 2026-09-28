

package com.bitselect.agent.mcp.dao.result;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 售后单连同对应订单行的商品名快照，商品改名不影响历史售后
 */
@Data
public class AfterSaleDetailResult {

    private String afterSaleNo;

    private String orderNo;

    private String skuCode;

    private String skuName;

    private String type;

    private String reason;

    private String status;

    private Timestamp createTime;

    private Timestamp updateTime;

    private Timestamp finishTime;
}
