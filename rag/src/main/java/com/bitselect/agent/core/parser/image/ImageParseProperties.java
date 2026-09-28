package com.bitselect.agent.core.parser.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 【文件用途】图片解析（图生文）的配置类：从 application.yaml 的 rag.image-parse 节点读取。
 *
 * 【为什么存在】
 * - 图生文的 prompt、token 上限、是否启用内嵌图图生文，都需要可配置
 * - 不同业务场景对图生文的要求不同（客服场景要详细 OCR，设计稿场景要结构描述）
 *
 * 【配置项】
 * - descriptionPrompt：VLM 的引导提示词
 * - maxOutputTokens：描述 token 上限，控成本与 embedding 体量
 * - embeddedDescribeEnabled：文档内嵌图是否也做图生文
 *
 * 【被谁引用】
 * - ImageDocumentParser（解析图片时读配置）
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.image-parse")
public class ImageParseProperties {

    /**
     * 【配置项】图生文引导提示词：要求 VLM 输出中文描述 + 图中文字 OCR。
     */
    private String descriptionPrompt = "请用中文详细描述这张图片的内容；若图中包含文字，请逐字识别并完整列出（OCR）。"
            + "先给出整体内容描述，再用\"图中文字：\"另起一段列出识别到的所有文字。";

    /**
     * 【配置项】描述输出 token 上限，控成本与 embedding 体量；<=0 表示不限制。
     */
    private Integer maxOutputTokens = 1024;

    /**
     * 【配置项】是否给文档内嵌图也做图生文。
     *
     * 关掉则内嵌图的向量文本回落成一条图片 URL，等于永远召回不到；
     * 开着的代价是每张图一次 VLM 调用。
     */
    private boolean embeddedDescribeEnabled = true;
}