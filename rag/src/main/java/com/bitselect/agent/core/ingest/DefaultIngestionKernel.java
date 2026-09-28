package com.bitselect.agent.core.ingest;

import com.bitselect.agent.core.chunk.ChunkingService;
import com.bitselect.agent.core.chunk.model.Chunk;
import com.bitselect.agent.core.chunk.model.EmbeddedChunk;
import com.bitselect.agent.core.ingest.embed.ChunkEmbeddingService;
import com.bitselect.agent.core.ingest.sink.ChunkIndexWriter;
import com.bitselect.agent.core.parser.DocumentParser;
import com.bitselect.agent.core.parser.mime.MimeTypeDetector;
import com.bitselect.agent.core.parser.model.Block;
import com.bitselect.agent.core.parser.model.ParsedDocument;
import com.bitselect.agent.core.parser.registry.ParserRegistry;
import com.bitselect.agent.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 【文件用途】摄取内核默认实现：固定五步骨架，全文唯一一条摄取执行序列。
 *
 * 【为什么存在】
 * - IngestionKernel 接口的默认实现，是入库 Pipeline 的核心执行引擎
 * - 五步骨架严格顺序，不可跳过、不可换序
 * - 入口不收 MIME 也不收嵌入模型，任务状态与摄取日志一概不碰
 *
 * 【五步流程】
 *   ① identity：MimeTypeDetector 探测真实类型
 *   ② parse：ParserRegistry 按 (MIME × 档位) 找解析器
 *   ③ chunk：ChunkingService 按预算切分
 *   ④ embed：ChunkEmbeddingService 向量化并校验维度
 *   ⑤ index：ChunkIndexWriter 扇出到全部落点
 *
 * 【关键设计】
 * - 全链路唯一一次类型识别（identity）：避免下游多处判断 MIME
 * - parserOptions 必传 docId：解析器用它给图片资产命名，漏传则资产与文档失联
 * - 每步记录耗时用于 IngestionOutcome.timings
 *
 * 【被谁引用】IngestionNode（入库 Pipeline 的摄取节点）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultIngestionKernel implements IngestionKernel {

    /**
     * 解析器 options 键：原始文件名，写进块来源信息
     */
    private static final String OPT_SOURCE_FILE = "sourceFile";

    /**
     * 解析器 options 键：文档 ID，决定图片资产的归属目录 assets/{docId}/...
     */
    private static final String OPT_DOCUMENT_ID = "documentId";

    private final ParserRegistry parserRegistry;
    private final ChunkingService chunkingService;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final ChunkIndexWriter chunkIndexWriter;

    @Override
    public IngestionOutcome run(DocumentRef doc,
                                byte[] bytes,
                                IngestionSpec spec,
                                VectorTarget target) {
        if (bytes == null || bytes.length == 0) {
            throw new ClientException("文件内容为空：docId=" + doc.docId());
        }
        IngestionSpec effectiveSpec = spec == null ? IngestionSpec.defaults() : spec;

        // ① identity：全链路唯一一次类型识别
        String mimeType = MimeTypeDetector.detect(bytes, doc.filename());
        if (!StringUtils.hasText(mimeType)) {
            throw new ClientException("无法识别文件类型：docId=" + doc.docId() + ", filename=" + doc.filename());
        }

        // ② parse：(MIME × 档位) → 解析器
        long parseStart = System.currentTimeMillis();
        DocumentParser parser = parserRegistry.require(mimeType, effectiveSpec.parseProfile());
        ParsedDocument parsed = parser.parseStructured(bytes, mimeType, parserOptions(doc));
        List<Block> blocks = parsed.blocks() == null ? List.of() : parsed.blocks();
        long parseMillis = System.currentTimeMillis() - parseStart;
        log.info("摄取-解析完成 docId={} mime={} 档位={} 解析器={} blocks={}",
                doc.docId(), mimeType, effectiveSpec.parseProfile().getCode(), parser.getParserType(), blocks.size());

        // ③ chunk：Block 类型 → chunker + 预算
        long chunkStart = System.currentTimeMillis();
        List<Chunk> chunks = chunkingService.chunk(blocks, effectiveSpec.budget());
        long chunkMillis = System.currentTimeMillis() - chunkStart;

        if (chunks.isEmpty()) {
            throw new ClientException("分块结果为空：docId=" + doc.docId() + ", mime=" + mimeType);
        }

        // ④ embed：模型与维度都来自落点，此处校验维度
        long embedStart = System.currentTimeMillis();
        List<EmbeddedChunk> embedded = chunkEmbeddingService.embed(chunks, target);
        long embedMillis = System.currentTimeMillis() - embedStart;

        // ⑤ index：扇出到全部落点，事务边界在写入器内
        long indexStart = System.currentTimeMillis();
        chunkIndexWriter.replaceDocument(target, doc, embedded);
        long indexMillis = System.currentTimeMillis() - indexStart;

        return new IngestionOutcome(mimeType, parser.getParserType(), blocks.size(), chunks,
                new IngestionOutcome.IngestionTimings(parseMillis, chunkMillis, embedMillis, indexMillis));
    }

    /**
     * 【方法用途】组装解析器入参。
     *
     * docId 必须传，解析器用它给图片资产命名，漏传则资产与文档失联。
     */
    private Map<String, Object> parserOptions(DocumentRef doc) {
        Map<String, Object> options = new HashMap<>();
        if (StringUtils.hasText(doc.filename())) {
            options.put(OPT_SOURCE_FILE, doc.filename());
        }
        options.put(OPT_DOCUMENT_ID, doc.docId());
        return options;
    }
}