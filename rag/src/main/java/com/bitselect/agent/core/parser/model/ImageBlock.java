package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】图片 Block：文档中的一张图片。
 *
 * 【为什么存在】
 * - 图片链接（markdown 语法 ![alt](url)）被切碎会导致前端渲染失败
 * - 图片可能需要 VLM 图生文（description），把图片内容转成可检索的文本
 *
 * 【分块策略】
 * - 由 ImageChunker 渲染成 ![caption](url) 的 atomic chunk，永不切分
 *
 * 【description 的作用】
 * - 同时用于 embedding 检索（让图片能被文字搜到）与喂 LLM 答题
 * - MinerU 等不产图生文的来源为 null
 *
 * @param provenance  来源信息
 * @param asset       图片的资源引用（URL + MIME）
 * @param caption     图片标题（markdown 中括号里的内容）
 * @param altText     图片 alt 文本
 * @param description VLM 图生文结果，无则为 null
 */
public record ImageBlock(
        Provenance provenance,
        AssetRef asset,
        String caption,
        String altText,
        String description
) implements Block {

    /**
     * 便捷构造：不产图生文的来源（MinerU / Excel 等）用此形态，description 置空。
     */
    public ImageBlock(Provenance provenance, AssetRef asset, String caption, String altText) {
        this(provenance, asset, caption, altText, null);
    }
}