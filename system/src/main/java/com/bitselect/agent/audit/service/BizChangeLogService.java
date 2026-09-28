

package com.bitselect.agent.audit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bitselect.agent.audit.controller.request.BizChangeLogPageRequest;
import com.bitselect.agent.audit.controller.vo.BizChangeLogVO;

public interface BizChangeLogService {

    IPage<BizChangeLogVO> page(BizChangeLogPageRequest requestParam);

    BizChangeLogVO get(String id);
}
