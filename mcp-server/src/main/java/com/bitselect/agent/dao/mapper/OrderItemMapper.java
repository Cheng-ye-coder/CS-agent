

package com.bitselect.agent.mcp.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.mcp.dao.entity.OrderItemDO;

/**
 * 订单行，商品名与单价都是下单当时的快照
 */
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
}
