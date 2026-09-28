package com.bitselect.agent.core.parser.excel;

import com.bitselect.agent.core.parser.DocumentParser;
import com.bitselect.agent.core.parser.ParserType;
import com.bitselect.agent.core.parser.excel.ExcelTableNormalizer.NormalizedTable;
import com.bitselect.agent.core.parser.model.Block;
import com.bitselect.agent.core.parser.model.HeadingBlock;
import com.bitselect.agent.core.parser.model.ParsedDocument;
import com.bitselect.agent.core.parser.model.Provenance;
import com.bitselect.agent.core.parser.model.TableBlock;
import com.bitselect.agent.core.parser.registry.ParseProfile;
import com.bitselect.agent.framework.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】Excel 文档解析器（Apache POI）：把 xlsx / xls 解析成 Block 列表。
 *
 * 【为什么存在】
 * - Excel 是知识库里最常见的表格格式（数据清单、财务报表、配置表）
 * - Excel 的单元格元数据（超链接、合并单元格、多行表头）只有 POI 能拿到，OCR / MinerU 都拿不到
 * - 每个 sheet 解析为一张 TableBlock + 一个 HeadingBlock（承载 sheet 名）
 *
 * 【关键设计】
 * - 单元格规范化交给 ExcelTableNormalizer
 * - sheet 名走 HeadingBlock（H1 级别）而不是自建上下文字段：
 *   H1 的顶级重置语义正好是 sheet 之间的关系，交给 HeadingHandler 维护 outlinePath 后，
 *   sheet 名自然落到向量文本前缀、outline 列与 ES outline 字段
 * - 跳过隐藏 sheet（workbook.isSheetHidden / isSheetVeryHidden）
 * - 认领了 Tika 的两个 Office 家族别名（application/x-tika-msoffice / x-tika-ooxml），
 *   因为纯字节探测（无文件名）时 xlsx / doc 都回落到它们且无法再区分，交给 POI 通用读取
 *
 * 【被谁引用】ParserRegistry 启动时收集，运行时按 MIME 查找。
 */
@Slf4j
@Component
public class ExcelDocumentParser implements DocumentParser {

    public static final String OPT_SOURCE_FILE = "sourceFile";
    public static final String OPT_HEADER_ROWS = "headerRows";

    private static final int DEFAULT_HEADER_ROWS = 1;

    @Override
    public String getParserType() {
        return ParserType.EXCEL_POI.getType();
    }

    @Override
    public Map<ParseProfile, Set<String>> supportedMimeTypes() {
        return Map.of(ParseProfile.FAST, Set.of(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "application/x-tika-msoffice",
                "application/x-tika-ooxml"
        ));
    }

    @Override
    public ParsedDocument parseStructured(byte[] content, String mimeType, Map<String, Object> options) {
        if (content == null || content.length == 0) {
            return ParsedDocument.of(List.of());
        }

        String sourceFile = extractString(options);
        int headerRows = extractInt(options);

        List<Block> blocks = new ArrayList<>();
        int totalSheets;

        try (ByteArrayInputStream is = new ByteArrayInputStream(content);
             Workbook workbook = WorkbookFactory.create(is)) {

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            totalSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < totalSheets; i++) {
                if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) {
                    log.info("跳过隐藏 sheet[{}]，不纳入解析结果", workbook.getSheetName(i));
                    continue;
                }
                Sheet sheet = workbook.getSheetAt(i);
                blocks.addAll(buildSheetBlocks(sheet, sourceFile, headerRows, formatter, evaluator));
            }
        } catch (Exception e) {
            log.error("Excel 解析失败，MIME 类型: {}, 文件大小: {} bytes", mimeType, content.length, e);
            throw new ServiceException("Excel 解析失败: " + e.getMessage());
        }

        return ParsedDocument.of(blocks, Map.of(
                "parser", getParserType(),
                "mimeType", mimeType == null ? "" : mimeType,
                "totalSheets", totalSheets,
                // 只数表格：每个 sheet 还额外产一个承载 sheet 名的 HeadingBlock
                "parsedTables", blocks.stream().filter(TableBlock.class::isInstance).count(),
                "headerRows", headerRows
        ));
    }

    /**
     * 【方法用途】规范化 sheet 为单张表，产出 0 或 2 个 Block。
     */
    private List<Block> buildSheetBlocks(Sheet sheet, String sourceFile, int headerRows,
                                         DataFormatter formatter, FormulaEvaluator evaluator) {
        NormalizedTable table = ExcelTableNormalizer.normalize(sheet, formatter, evaluator, headerRows);
        if (table.isEmpty()) {
            log.debug("Sheet [{}] 为空，跳过", sheet.getSheetName());
            return List.of();
        }

        Provenance prov = Provenance.ofExcelCell(sourceFile, sheet.getSheetName());
        return List.of(
                new HeadingBlock(prov, 1, sheet.getSheetName()),
                new TableBlock(prov, table.headers(), table.rows())
        );
    }

    private static String extractString(Map<String, Object> options) {
        if (options == null) {
            return "";
        }
        Object v = options.get(ExcelDocumentParser.OPT_SOURCE_FILE);
        return v == null ? "" : v.toString();
    }

    private static int extractInt(Map<String, Object> options) {
        if (options == null) {
            return ExcelDocumentParser.DEFAULT_HEADER_ROWS;
        }
        Object v = options.get(ExcelDocumentParser.OPT_HEADER_ROWS);
        if (v == null) {
            return ExcelDocumentParser.DEFAULT_HEADER_ROWS;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return ExcelDocumentParser.DEFAULT_HEADER_ROWS;
        }
    }
}