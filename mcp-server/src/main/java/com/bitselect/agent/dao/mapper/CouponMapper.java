

package com.bitselect.agent.mcp.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitselect.agent.mcp.dao.entity.CouponDO;

/**
 * 券模板
 * <p>
 * 单表查询一处都没有，持券关系一律走 UserCouponMapper 的联查；这个接口只为补齐一表一 Mapper
 */
public interface CouponMapper extends BaseMapper<CouponDO> {
}
