package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】标题 Block：文档中的一个标题（章节、小节、子小节）。
 *
 * 【为什么存在】
 * - 标题需要累积进"章节路径"，作为后续段落的 outlinePath 前缀
 * - 让每个段落都知道自己属于哪个章节，检索时能返回"来自第 3 章第 2 节"这样的来源
 *
 * 【关键设计】
 * - 记录 level（1-6）但【不参与比较级别】——不同解析器对级别的判定主观
 *   （MinerU 按字号猜，Markdown 按 # 数量），比较级别会导致不可控
 * - 标题自身【不产 chunk】，只被 HeadingHandler 消费，累积进上下文
 *
 * 【被谁引用】HeadingHandler（累积章节路径）。
 *
 * @param provenance 来源信息
 * @param level      markdown 标题级别，1-6
 * @param text       标题文本（不含 # 前缀）
 */
public record HeadingBlock(
        Provenance provenance,
        int level,
        String text
) implements Block {
}