package com.bitselect.agent.core.parser.mineru;

/**
 * 【文件用途】MinerU 任务状态快照：轮询接口返回的完整状态。
 *
 * 【为什么存在】
 * - 轮询时需要携带"状态 + 结果 URL + 错误信息"三要素
 * - 提供便捷方法 completed() / failed() 让轮询器判断逻辑更清晰
 *
 * 【被谁引用】MinerUClient（构造）、MinerUPollingExecutor（判断）。
 *
 * @param state        当前状态
 * @param zipUrl       结果 zip 下载 URL，仅 DONE 时非空
 * @param errorMessage 失败原因，仅 FAILED 时非空
 */
public record MinerUStatus(
        MinerUTaskState state,
        String zipUrl,
        String errorMessage
) {

    public boolean completed() {
        return state == MinerUTaskState.DONE;
    }

    public boolean failed() {
        return state == MinerUTaskState.FAILED;
    }
}