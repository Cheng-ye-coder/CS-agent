package com.bitselect.agent.core.parser;

import com.bitselect.agent.core.parser.model.AssetRef;
import com.bitselect.agent.core.parser.model.Block;
import com.bitselect.agent.core.parser.model.CodeBlock;
import com.bitselect.agent.core.parser.model.HeadingBlock;
import com.bitselect.agent.core.parser.model.HtmlTableBlock;
import com.bitselect.agent.core.parser.model.ImageBlock;
import com.bitselect.agent.core.parser.model.ListBlock;
import com.bitselect.agent.core.parser.model.ParagraphBlock;
import com.bitselect.agent.core.parser.model.ParsedDocument;
import com.bitselect.agent.core.parser.model.Provenance;
import com.bitselect.agent.core.parser.registry.ParseProfile;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 【文件用途】Markdown 文档解析器：用 commonmark-java 解析 AST，按节点类型产出对应 Block。
 *
 * 【为什么存在】
 * - Markdown 是知识库最常见的文档格式（技术文档、说明文档）
 * - Markdown 自带结构信息（标题层级、代码块、列表、表格），直接当纯文本处理会丢失结构
 * - 用 AST 解析比正则匹配可靠得多
 *
 * 【支持节点】
 * - Heading → HeadingBlock
 * - Paragraph → ParagraphBlock（含内嵌链接、图片、行内代码）
 * - FencedCodeBlock / IndentedCodeBlock → CodeBlock
 * - BulletList / OrderedList → ListBlock
 * - GFM Table → TableBlock
 * - HtmlBlock → HtmlTableBlock（若是表格）或 ParagraphBlock
 * - 独占一行的图片 → ImageBlock
 *
 * 【关键设计】
 * - commonmark Parser 线程安全，静态共享
 * - 只处理顶层 block，不递归进嵌套（列表项内的代码块仍归 ListBlock）
 * - HTML 表格另立块类型，否则会被按字符硬切、断面停在标签中间
 * - 内嵌 HTML 必须被显式处理，否则基类的 visitChildren 会静默丢弃
 *
 * 【MIME 认领】
 * - text/x-web-markdown：Tika 探测 .md 的产出
 * - text/markdown / text/x-markdown：来自外部 Content-Type
 * - text/plain：txt 也走本解析器，让缩进段落与列表至少能拿到结构
 *
 * 【被谁引用】ParserRegistry 启动时收集，运行时按 MIME 查找。
 */
@Component
public class MarkdownDocumentParser implements DocumentParser {

    /**
     * commonmark 解析器，线程安全可共享。
     */
    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    @Override
    public String getParserType() {
        return ParserType.MARKDOWN.getType();
    }

    @Override
    public ParsedDocument parseStructured(byte[] content, String mimeType, Map<String, Object> options) {
        if (content == null || content.length == 0) {
            return ParsedDocument.of(List.of());
        }

        String text = new String(content, StandardCharsets.UTF_8);
        Provenance prov = Provenance.ofFile(extractSourceFile(options));

        Document doc = (Document) PARSER.parse(text);
        BlockExtractingVisitor visitor = new BlockExtractingVisitor(prov);
        doc.accept(visitor);

        return ParsedDocument.of(visitor.getBlocks(), Map.of(
                "parser", getParserType(),
                "mimeType", mimeType == null ? "" : mimeType,
                "blocks", visitor.getBlocks().size()
        ));
    }

    @Override
    public Map<ParseProfile, Set<String>> supportedMimeTypes() {
        return Map.of(ParseProfile.FAST, Set.of(
                "text/x-web-markdown",
                "text/markdown",
                "text/x-markdown",
                "text/plain"
        ));
    }

    private static String extractSourceFile(Map<String, Object> options) {
        if (options == null) {
            return "";
        }
        Object v = options.get("sourceFile");
        return v == null ? "" : v.toString();
    }

    // ===================== AST Visitor =====================

    /**
     * 【内部类用途】AST 访问器：commonmark 节点 → ragent Block。
     *
     * 只处理顶层 block，不递归进嵌套（列表项内的代码块仍归 ListBlock）。
     */
    private static final class BlockExtractingVisitor extends AbstractVisitor {

        private final Provenance provenance;
        private final List<Block> blocks = new ArrayList<>();

        BlockExtractingVisitor(Provenance provenance) {
            this.provenance = provenance;
        }

        List<Block> getBlocks() {
            return blocks;
        }

        @Override
        public void visit(Heading heading) {
            blocks.add(new HeadingBlock(
                    provenance,
                    heading.getLevel(),
                    extractInlineText(heading)
            ));
            // 不向下递归，标题内的 inline 已合并
        }

        @Override
        public void visit(Paragraph paragraph) {
            // 只处理顶层段落，列表项内的段落归 ListBlock
            if (paragraph.getParent() instanceof ListItem) {
                return;
            }
            // 独占一行的图片按图片块产出，而不是压成一段只剩 alt 文本的文字
            Image standaloneImage = asStandaloneImage(paragraph);
            if (standaloneImage != null) {
                blocks.add(toImageBlock(standaloneImage, provenance));
                return;
            }
            String text = extractInlineText(paragraph);
            if (!text.isEmpty()) {
                blocks.add(new ParagraphBlock(provenance, text));
            }
        }

        /**
         * 内嵌 HTML：HtmlBlock 是叶子节点、内容只在 literal 里，不接管就被基类的
         * visitChildren 静默丢弃。表格另立块类型，落成段落会被按字符硬切、断面停在标签中间。
         */
        @Override
        public void visit(HtmlBlock htmlBlock) {
            String html = htmlBlock.getLiteral() == null ? "" : htmlBlock.getLiteral().strip();
            if (html.isEmpty()) {
                return;
            }
            blocks.add(html.regionMatches(true, 0, "<table", 0, 6)
                    ? new HtmlTableBlock(provenance, html)
                    : new ParagraphBlock(provenance, html));
        }

        @Override
        public void visit(FencedCodeBlock codeBlock) {
            blocks.add(new CodeBlock(
                    provenance,
                    codeBlock.getInfo(),
                    stripTrailingNewline(codeBlock.getLiteral())
            ));
        }

        @Override
        public void visit(IndentedCodeBlock codeBlock) {
            blocks.add(new CodeBlock(
                    provenance,
                    null,
                    stripTrailingNewline(codeBlock.getLiteral())
            ));
        }

        @Override
        public void visit(BulletList bulletList) {
            blocks.add(buildListBlock(bulletList, false));
            // 不向下递归
        }

        @Override
        public void visit(OrderedList orderedList) {
            blocks.add(buildListBlock(orderedList, true));
            // 不向下递归
        }

        @Override
        public void visit(org.commonmark.node.CustomBlock customBlock) {
            // GFM TableBlock 是 CustomBlock 子类
            if (customBlock instanceof org.commonmark.ext.gfm.tables.TableBlock tableBlock) {
                handleTable(tableBlock);
                return;
            }
            super.visit(customBlock);
        }

        private ListBlock buildListBlock(Node listNode, boolean ordered) {
            List<String> items = new ArrayList<>();
            Node child = listNode.getFirstChild();
            while (child != null) {
                if (child instanceof ListItem) {
                    items.add(extractInlineText(child).trim());
                }
                child = child.getNext();
            }
            return new ListBlock(provenance, ordered, items);
        }

        private void handleTable(org.commonmark.ext.gfm.tables.TableBlock tableBlock) {
            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            Node child = tableBlock.getFirstChild();
            while (child != null) {
                if (child instanceof TableHead head) {
                    Node hr = head.getFirstChild();
                    if (hr instanceof TableRow tr) {
                        headers.addAll(extractCellTexts(tr));
                    }
                } else if (child instanceof TableBody body) {
                    Node tr = body.getFirstChild();
                    while (tr != null) {
                        if (tr instanceof TableRow row) {
                            rows.add(extractCellTexts(row));
                        }
                        tr = tr.getNext();
                    }
                }
                child = child.getNext();
            }

            // 【注意】用全限定名避免与 commonmark 的 TableBlock 冲突
            blocks.add(new com.bitselect.agent.core.parser.model.TableBlock(
                    provenance,
                    headers,
                    rows
            ));
        }

        private List<String> extractCellTexts(TableRow row) {
            List<String> cells = new ArrayList<>();
            Node cell = row.getFirstChild();
            while (cell != null) {
                if (cell instanceof TableCell tc) {
                    cells.add(extractInlineText(tc).trim());
                }
                cell = cell.getNext();
            }
            return cells;
        }
    }

    /**
     * 【方法用途】拼接节点内所有 inline 文本（Text / Code / Link / Emphasis 等）。
     *
     * Link 保留 [text](url) 形式以与下游 ImageChunker 风格一致。
     */
    private static String extractInlineText(Node parent) {
        StringBuilder sb = new StringBuilder();
        Node child = parent.getFirstChild();
        while (child != null) {
            appendInline(sb, child);
            child = child.getNext();
        }
        return sb.toString();
    }

    private static void appendInline(StringBuilder sb, Node node) {
        if (node instanceof Text t) {
            sb.append(t.getLiteral());
        } else if (node instanceof Code code) {
            sb.append('`').append(code.getLiteral()).append('`');
        } else if (node instanceof Link link) {
            String inner = extractInlineText(link);
            String dest = link.getDestination();
            sb.append('[').append(inner).append("](").append(dest).append(')');
        } else if (node instanceof Image image) {
            // 必须带上 URL：只剩 alt 文本的话，md 里的图在知识库中既不可引用也不可预览
            sb.append("![").append(extractInlineText(image)).append("](")
                    .append(image.getDestination() == null ? "" : image.getDestination()).append(')');
        } else if (node instanceof Emphasis || node instanceof StrongEmphasis) {
            // 保留 inline 文本，丢掉 markdown 强调标记
            sb.append(extractInlineText(node));
        } else if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
            sb.append('\n');
        } else if (node.getFirstChild() != null) {
            Node child = node.getFirstChild();
            while (child != null) {
                appendInline(sb, child);
                child = child.getNext();
            }
        }
    }

    /**
     * 【方法用途】判断段落是否只包含一张图片（允许周围有空白文本）。
     */
    private static Image asStandaloneImage(Paragraph paragraph) {
        Image found = null;
        Node child = paragraph.getFirstChild();
        while (child != null) {
            if (child instanceof Image image) {
                if (found != null) {
                    return null;
                }
                found = image;
            } else if (!(child instanceof SoftLineBreak || child instanceof HardLineBreak)
                    && !(child instanceof Text text && text.getLiteral().isBlank())) {
                return null;
            }
            child = child.getNext();
        }
        return found;
    }

    /**
     * 【方法用途】图片节点 → 图片块。
     *
     * 地址是作者写的原样地址（外链或相对路径），不经过资产上传，因此没有图生文描述，
     * 向量文本由分块阶段回落到链接本身。
     */
    private static ImageBlock toImageBlock(Image image, Provenance provenance) {
        String url = image.getDestination() == null ? "" : image.getDestination();
        String altText = extractInlineText(image);
        return new ImageBlock(
                provenance,
                new AssetRef(url, guessImageMime(url)),
                image.getTitle(),
                altText
        );
    }

    /**
     * 【方法用途】按地址后缀猜 MIME：图片地址不经过字节探测，只能按扩展名给一个合理值。
     */
    private static String guessImageMime(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return null;
    }

    private static String stripTrailingNewline(String s) {
        if (s == null) {
            return "";
        }
        if (s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}