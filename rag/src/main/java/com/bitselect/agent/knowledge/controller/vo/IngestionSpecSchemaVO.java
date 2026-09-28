package com.bitselect.agent.knowledge.controller.vo;

import java.util.List;

/**
 * 【文件用途】文档级摄取配置的表单 schema：后端下发给前端的动态表单定义。
 *
 * 【为什么存在】
 * - 前端不该硬编码"有哪些档位、每个字段的合法范围是多少"
 * - 取值范围的权威在 ChunkBudget 的构造期，此处只是把同一份数字告诉前端
 * - 取代原先的"分块策略列表"：策略枚举在真实链路上不产生任何差异，
 *   切法由文档结构唯一决定，用户只控预算
 * - 后端加一个参数不需要改前端
 *
 * 【被谁引用】
 * - IngestionSpecSchemaProvider（构造）
 * - 前端表单渲染（通过 Controller 返回）
 *
 * @param parseProfileLabel      档位选项的字段名
 * @param parseProfiles          可选解析档位
 * @param parseProfileExtensions 档位真正有区别的文件扩展名
 * @param budgetFields           分块预算字段定义
 * @param wholeDocumentSentinel  整文档不分块的哨兵取值
 */
public record IngestionSpecSchemaVO(String parseProfileLabel,
                                    List<Option> parseProfiles,
                                    List<String> parseProfileExtensions,
                                    List<BudgetField> budgetFields,
                                    int wholeDocumentSentinel) {

    /**
     * 【内部结构】单个档位选项。
     *
     * @param value 提交值
     * @param label 展示名
     * @param hint  说明
     */
    public record Option(String value, String label, String hint) {
    }

    /**
     * 【内部结构】单个预算字段的定义。
     *
     * @param key            提交键
     * @param label          展示名
     * @param defaultValue   默认值
     * @param min            允许最小值，越界由构造期拦下
     * @param max            允许最大值，越界由构造期拦下
     * @param recommendedMin 建议区间下界
     * @param recommendedMax 建议区间上界
     * @param hint           一句话说明，常驻展示
     * @param detail         调参说明，前端收进悬浮层
     */
    public record BudgetField(String key, String label, int defaultValue, int min, int max,
                              int recommendedMin, int recommendedMax, String hint, String detail) {
    }
}