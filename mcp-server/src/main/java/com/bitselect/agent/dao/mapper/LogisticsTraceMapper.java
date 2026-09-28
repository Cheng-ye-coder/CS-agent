

package com.bitselect.agent.mcp.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bitselect.agent.mcp.dao.entity.LogisticsTraceDO;

import java.util.List;

/**
 * 物流轨迹
 * <p>
 * 这里只按运单号取轨迹，归属校验在订单那一步做完：只凭运单号就能取到轨迹的话，
 * 运单号本身就成了越权入口
 */
public interface LogisticsTraceMapper extends BaseMapper<LogisticsTraceDO> {

    default List<LogisticsTraceDO> selectByTrackingNo(String trackingNo) {
        return selectList(Wrappers.<LogisticsTraceDO>lambdaQuery()
                .eq(LogisticsTraceDO::getTrackingNo, trackingNo)
                .orderByDesc(LogisticsTraceDO::getTraceTime));
    }
}
