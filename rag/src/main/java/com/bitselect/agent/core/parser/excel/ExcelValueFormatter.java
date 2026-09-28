package com.bitselect.agent.core.parser.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 【文件用途】Excel cell 值格式化工具：把 POI 的 Cell 转成字符串，处理公式回退与删除线。
 *
 * 【为什么存在】
 * - POI 的 Cell 有多种类型（数值、日期、布尔、公式、错误），直接 toString 会出错
 * - 公式 cell 求值可能失败（如引用了外部文件），需要三级回退策略
 * - "删除线 = 软删除"是业务约定，需要能识别
 *
 * 【公式 cell 的三级回退】
 * 1. 通过 FormulaEvaluator 求值（最准确）
 * 2. 读 cached formula result（上次写入时的缓存值）
 * 3. 读原始公式字符串（如 "=SUM(A1:A10)"）
 *
 * 【被谁引用】
 * - ExcelTableNormalizer.readGrid（读每个 cell 时调用）
 */
@Slf4j
public final class ExcelValueFormatter {

    private ExcelValueFormatter() {
    }

    /**
     * 【方法用途】格式化 cell 为字符串。
     *
     * @param cell      cell 实例，可空（返回空字符串）
     * @param formatter DataFormatter 实例（线程不安全，调用方持有）
     * @param evaluator 公式求值器，可空（无 evaluator 时公式 cell 走缓存值或公式字符串）
     * @return 格式化后的字符串（已 trim）
     */
    public static String format(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }

        if (cell.getCellType() == CellType.FORMULA) {
            return formatFormulaCell(cell, formatter, evaluator);
        }

        return formatter.formatCellValue(cell).trim();
    }

    private static String formatFormulaCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        // 第 1 选择：通过 evaluator 求值
        if (evaluator != null) {
            try {
                return formatter.formatCellValue(cell, evaluator).trim();
            } catch (Exception e) {
                log.warn("公式 cell evaluate 失败，回退到缓存值。cell: {}", describe(cell), e);
            }
        }

        // 第 2 选择：直接读 cached formula result（POI 5.x 默认会缓存上次写入时的结果）
        try {
            CellType cachedType = cell.getCachedFormulaResultType();
            return switch (cachedType) {
                case NUMERIC -> formatter.formatCellValue(cell).trim();
                case STRING -> cell.getStringCellValue().trim();
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case ERROR -> "";
                default -> cell.getCellFormula();
            };
        } catch (Exception e) {
            log.warn("读取公式缓存值失败，回退到公式字符串。cell: {}", describe(cell), e);
        }

        // 第 3 选择：原始公式字符串
        try {
            return cell.getCellFormula();
        } catch (Exception e) {
            log.warn("读取公式字符串失败，返回空。cell: {}", describe(cell), e);
            return "";
        }
    }

    /**
     * 【方法用途】判断 cell 是否被划删除线（字体级 strikeout）。
     *
     * 业务里"删除线 = 软删除"约定，整行划线即整行 cell 字体 strikeout；
     * 按 cell 字体判定，XSSF / HSSF 通用。富文本局部划线不在此判定范围。
     */
    public static boolean isStrikethrough(Cell cell) {
        if (cell == null) {
            return false;
        }
        try {
            CellStyle style = cell.getCellStyle();
            if (style == null) {
                return false;
            }
            Workbook workbook = cell.getSheet().getWorkbook();
            Font font = workbook.getFontAt(style.getFontIndex());
            return font != null && font.getStrikeout();
        } catch (Exception e) {
            return false;
        }
    }

    private static String describe(Cell cell) {
        return cell.getSheet().getSheetName()
                + "!" + cell.getAddress().formatAsString();
    }
}