package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.chunk.model.ChunkDraft;
import com.bitselect.agent.core.chunk.model.ChunkMetadata;
import com.bitselect.agent.core.parser.model.AssetRef;
import com.bitselect.agent.core.parser.model.ImageBlock;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 【文件用途】图片 chunker：一图一块，展示文本是描述 + markdown 图片链接，向量文本只取描述。
 *
 * 【为什么存在】
 * - 图片 URL 进向量是纯噪声（相同 URL 前缀会污染语义空间），只在无描述时才回落到链接本身
 * - 图片链接被切碎会导致前端渲染失败，必须 atomic
 * - 声明为"可流动"：让图与它的前导语 / 解释文字同块，检索命中即带图
 *
 * 【关键设计】
 * - 展示文本 = description + markdown 链接，供前端渲染
 * - 向量文本只取 description（无 URL 噪声）
 * - 无 description 时（MinerU 抽图）向量文本回落到 markdown 链接
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（dispatch 中调用）。
 */
@Component
public class ImageChunker implements BlockChunker<ImageBlock> {

    @Override
    public Class<ImageBlock> blockType() {
        return ImageBlock.class;
    }

    @Override
    public List<ChunkDraft> chunk(ImageBlock block, ChunkContext ctx) {
        if (block == null || block.asset() == null) {
            return List.of();
        }
        AssetRef asset = block.asset();
        String markdown = "![" + pickCaption(block) + "](" + asset.publicUrl() + ")";

        String description = block.description();
        boolean hasDescription = description != null && !description.isBlank();
        String content = hasDescription ? description.strip() + "\n\n" + markdown : markdown;

        ChunkMetadata metadata = ChunkMetadata.builder()
                .outlinePath(ctx.outlinePath())
                .assets(List.of(asset))
                .provenance(block.provenance())
                .build();

        return List.of(ChunkDraft.of(content, hasDescription ? description.strip() : null, metadata));
    }

    private String pickCaption(ImageBlock block) {
        if (block.caption() != null && !block.caption().isEmpty()) {
            return block.caption();
        }
        if (block.altText() != null && !block.altText().isEmpty()) {
            return block.altText();
        }
        return "";
    }
}