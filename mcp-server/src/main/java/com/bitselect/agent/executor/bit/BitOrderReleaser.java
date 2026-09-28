

package com.bitselect.agent.mcp.executor.bit;

import com.bitselect.agent.mcp.dao.mapper.OrderMapper;
import com.bitselect.agent.mcp.dao.mapper.ProductSkuMapper;
import com.bitselect.agent.mcp.dao.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 取消订单并归还它占住的库存与券
 * <p>
 * 用户主动取消和超时释放共用这一个方法：写成两套一定会漂，
 * 漂出来的症状是「有时候券退回来了有时候没有」，最难查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BitOrderReleaser {

    static final String STATUS_PENDING = "待支付";
    static final String STATUS_PAID = "已支付待发货";
    static final String STATUS_CANCELLED = "已取消";

    private final OrderMapper orderMapper;
    private final ProductSkuMapper productSkuMapper;
    private final UserCouponMapper userCouponMapper;
    private final TransactionTemplate bitTransactionTemplate;

    /**
     * 带期望状态的条件取消：抢不到那一行就什么都不做
     * <p>
     * 用户点支付和定时任务扫超时可能撞在一起，谁的受影响行数是 1 谁赢，
     * 输的一方拿 false 回去照实说，而不是抛异常
     */
    public boolean release(String orderNo, String expectedStatus) {
        return Boolean.TRUE.equals(bitTransactionTemplate.execute(status -> {
            int cancelled = orderMapper.cancel(orderNo, expectedStatus);
            if (cancelled == 0) {
                return false;
            }
            int stockRows = productSkuMapper.restoreStockByOrder(orderNo);
            int couponRows = userCouponMapper.restoreByOrder(orderNo);
            log.info("订单已取消并释放资源, orderNo={}, from={}, 回补商品={}, 退回券={}",
                    orderNo, expectedStatus, stockRows, couponRows);
            return true;
        }));
    }
}
