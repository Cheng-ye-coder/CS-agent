

package com.bitselect.agent.mcp.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bitselect.agent.mcp.dao.entity.TicketDO;

/**
 * 人工工单
 */
public interface TicketMapper extends BaseMapper<TicketDO> {

    /**
     * 按单号前缀数当天已有多少张，用来拼下一个流水号
     */
    default long countByNoPrefix(String prefix) {
        return selectCount(Wrappers.<TicketDO>lambdaQuery().likeRight(TicketDO::getTicketNo, prefix));
    }
}
