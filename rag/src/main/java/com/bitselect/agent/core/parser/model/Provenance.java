package com.bitselect.agent.core.parser.model;

/**
 * 【文件用途】Block 来源信息：记录"这个 Block 来自哪个文件的哪个位置"。
 *
 * 【为什么存在】
 * - 每个 Block 都必须能追溯到源头，用户说"这段回答错了"时要能定位
 * - 排查分块质量时，按 sourceFile 分组统计
 * - Excel 多 sheet 场景，需要区分是哪个 sheet
 *
 * 【关键设计】
 * - sheetName 只作溯源标记，【不参与向量文本】——sheet 名由 Excel 解析器
 *   另产一个 HeadingBlock 走章节路径
 * - 提供两个静态工厂方法，语义清晰比 new 更好读
 *
 * 【被谁引用】所有 Block 实现类的必需字段。
 *
 * @param sourceFile 原始文件标识，文件 ID 或文件名
 * @param sheetName  Excel sheet 名，非 Excel 来源为 null
 */
public record Provenance(String sourceFile, String sheetName) {

    /**
     * 普通文件来源（PDF / Word / Markdown 等）。
     */
    public static Provenance ofFile(String sourceFile) {
        return new Provenance(sourceFile, null);
    }

    /**
     * Excel 单元格来源，需要携带 sheet 名。
     */
    public static Provenance ofExcelCell(String sourceFile, String sheetName) {
        return new Provenance(sourceFile, sheetName);
    }
}