package com.bitselect.agent.core.chunk.blockaware;

import com.bitselect.agent.core.parser.model.HeadingBlock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 【文件用途】标题处理器：按原始 heading 级别弹栈，维护调度器持有的章节路径。
 *
 * 【为什么存在】
 * - 每个 Block 都需要知道"自己属于哪个章节"，但章节路径需要随标题动态变化
 * - 需要按 heading 级别正确计算"新标题挂在哪个父级下"
 *
 * 【关键设计】
 * - 无状态：摄取并发共用同一实例，路径由调用方持有并逐块传入
 * - Outline 同时保留 path 和 levels：只看路径深度无法判断新标题该挂在哪一级下，
 *   不以 H1 开头的文档会把同级章节层层嵌套
 * - update 逻辑：弹掉同级与更深的祖先，真正的父级是最近一个级别更小的标题
 *
 * 【被谁引用】BlockAwareChunkerDispatcher（每遇到 HeadingBlock 调用一次）。
 */
@Component
public class HeadingHandler {

    /**
     * 【内部结构】章节路径连同各级的原始 heading 级别。
     */
    public record Outline(List<String> path, List<Integer> levels) {

        public static final Outline EMPTY = new Outline(List.of(), List.of());

        public Outline {
            path = path == null ? List.of() : List.copyOf(path);
            levels = levels == null ? List.of() : List.copyOf(levels);
        }
    }

    /**
     * 【方法用途】根据 heading 更新章节路径，入参与返回值都不可变。
     */
    public Outline update(Outline current, HeadingBlock heading) {
        Outline base = current == null ? Outline.EMPTY : current;
        if (heading == null) {
            return base;
        }
        int level = Math.max(1, heading.level());

        int keep = base.levels().size();
        while (keep > 0 && base.levels().get(keep - 1) >= level) {
            keep--;
        }

        List<String> path = new ArrayList<>(base.path().subList(0, keep));
        List<Integer> levels = new ArrayList<>(base.levels().subList(0, keep));
        path.add(heading.text() == null ? "" : heading.text());
        levels.add(level);
        return new Outline(path, levels);
    }
}