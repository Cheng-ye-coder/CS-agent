package com.bitselect.agent.core.parser.model;

import java.util.List;

/**
 * 【文件用途】列表 Block：有序列表（1. 2. 3.）或无序列表（- - -）或 checklist。
 *
 * 【为什么存在】
 * - 列表项之间有语义关联，拆开检索会丢上下文（"步骤 2"单独出现没有意义）
 * - 需要保留 ordered 标记，前端才知道显示 1.2.3. 还是 ●
 *
 * 【分块策略】
 * - 短列表：整个列表作为一个 atomic chunk，不切
 * - 长列表：由 ListChunker 按项分组，每组保留列表语义
 *
 * @param provenance 来源信息
 * @param ordered    是否有序列表
 * @param items      列表项文本，每项不含前置标记符
 */
public record ListBlock(
        Provenance provenance,
        boolean ordered,
        List<String> items
) implements Block {
}