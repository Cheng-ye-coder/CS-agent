package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】代码块 Block：文档中一段完整的代码。
 *
 * 【为什么存在】
 * - 代码不能像普通段落那样随意切分，切碎后完全不可用（括号不匹配、语法错误）
 * - 需要保留语言标识（java / python / bash），供前端语法高亮
 *
 * 【分块策略】
 * - 由 CodeChunker 产出 atomic chunk
 * - 即使超过分块预算也不切，宁可超长也不破坏代码完整性
 *
 * 【被谁引用】ChunkerNode 遍历时识别此类型，走 CodeChunker 分支。
 *
 * @param provenance 来源信息
 * @param language   编程语言标识，可空（有些代码块没有语言标注）
 * @param code       完整代码文本
 */
public record CodeBlock(
        Provenance provenance,
        String language,
        String code
) implements Block {
}