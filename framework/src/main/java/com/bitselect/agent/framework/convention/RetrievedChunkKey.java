

package com.bitselect.agent.framework.convention;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

/**
 * 检索结果的稳定唯一标识
 * 优先用 id，缺 id 时对 text 做 SHA256
 */
public final class RetrievedChunkKey {

    private RetrievedChunkKey() {
    }

    public static String of(RetrievedChunk chunk) {
        return StrUtil.isNotBlank(chunk.getId())
                ? chunk.getId()
                : DigestUtil.sha256Hex(chunk.getText() == null ? "" : chunk.getText());
    }
}