package com.bitselect.agent.core.parser.mineru;

import com.bitselect.agent.framework.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 【文件用途】MinerU 共享轮询调度器：把 HTTP 轮询从业务线程剥离到独立调度池。
 *
 * 【为什么存在】
 * - MinerU 解析是异步任务，需要轮询查询状态直到完成
 * - 如果每个业务线程各自 sleep + 轮询，会浪费大量线程资源
 * - 用一个共享调度池执行所有轮询，业务线程只在 future.get() 上阻塞
 *
 * 【关键设计】
 * - 4 个调度线程共享处理所有轮询任务，数百个 outstanding 任务共用
 * - 最小轮询间隔 100ms（生产 5s），让测试场景能用短间隔
 * - future 完成时兜底取消调度任务，避免内存泄漏
 * - 优雅停机：等待最多 10s 让活动任务完成
 *
 * 【被谁引用】MinerUDocumentParser（等待 MinerU 结果）。
 */
@Slf4j
@Component
public class MinerUPollingExecutor {

    private static final int SCHEDULER_THREADS = 4;
    private static final long SHUTDOWN_AWAIT_SECONDS = 10;

    private final MinerUClient client;
    private final MinerUProperties properties;

    private ScheduledExecutorService scheduler;

    public MinerUPollingExecutor(MinerUClient client, MinerUProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        this.scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREADS, namedFactory());
        log.info("MinerUPollingExecutor 启动: schedulerThreads={}", SCHEDULER_THREADS);
    }

    /**
     * 【方法用途】提交任务并阻塞 await 直到完成。
     *
     * 调用方业务线程在 future.get() 上阻塞，但不消耗任何 HTTP / sleep 资源。
     *
     * @param batchId MinerU 分配的 batch_id
     * @param timeout 超时时长
     * @return CompletableFuture，完成时携带 DONE 状态的 MinerUStatus（含 zipUrl）
     */
    public CompletableFuture<MinerUStatus> submitAndAwait(String batchId, Duration timeout) {
        if (batchId == null || batchId.isBlank()) {
            CompletableFuture<MinerUStatus> failed = new CompletableFuture<>();
            failed.completeExceptionally(new ServiceException("batchId 不能为空"));
            return failed;
        }

        CompletableFuture<MinerUStatus> future = new CompletableFuture<>();
        Instant deadline = Instant.now().plus(timeout);

        ScheduledFuture<?>[] holder = new ScheduledFuture[1];
        Runnable poll = () -> doPoll(batchId, future, deadline, holder);

        // 最小间隔 100ms（生产配置 5s，这里宽松下限让测试场景能用短间隔）
        long intervalMs = Math.max(100L, properties.getPollIntervalSeconds() * 1000L);
        holder[0] = scheduler.scheduleAtFixedRate(poll, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        // future 完成时（无论成功失败）兜底取消调度任务
        future.whenComplete((status, throwable) -> {
            ScheduledFuture<?> task = holder[0];
            if (task != null) {
                task.cancel(false);
            }
        });

        return future;
    }

    private void doPoll(String batchId,
                        CompletableFuture<MinerUStatus> future,
                        Instant deadline,
                        ScheduledFuture<?>[] holder) {
        if (future.isDone()) {
            return;
        }
        try {
            MinerUStatus status = client.queryResult(batchId);
            if (status.completed()) {
                complete(future, status, holder);
            } else if (status.failed()) {
                completeExceptionally(future, new ServiceException(
                        "MinerU 任务失败 batchId=" + batchId + " err=" + status.errorMessage()), holder);
            } else if (Instant.now().isAfter(deadline)) {
                completeExceptionally(future,
                        new TimeoutException("MinerU 任务超时 batchId=" + batchId), holder);
            }
        } catch (Exception e) {
            // 瞬时网络错误不立即终止，等下一轮重试；超时由 deadline 检查兜底
            log.warn("MinerU 轮询临时异常 batchId={}: {}", batchId, e.getMessage());
            if (Instant.now().isAfter(deadline)) {
                completeExceptionally(future,
                        new ServiceException("MinerU 轮询持续失败到超时 batchId=" + batchId + ": " + e.getMessage()),
                        holder);
            }
        }
    }

    private void complete(CompletableFuture<MinerUStatus> future,
                          MinerUStatus status,
                          ScheduledFuture<?>[] holder) {
        if (future.complete(status)) {
            cancelPolling(holder);
        }
    }

    private void completeExceptionally(CompletableFuture<MinerUStatus> future,
                                       Throwable error,
                                       ScheduledFuture<?>[] holder) {
        if (future.completeExceptionally(error)) {
            cancelPolling(holder);
        }
    }

    private void cancelPolling(ScheduledFuture<?>[] holder) {
        ScheduledFuture<?> task = holder[0];
        if (task != null) {
            task.cancel(false);
        }
    }

    @PreDestroy
    void shutdown() {
        if (scheduler == null) {
            return;
        }
        log.info("MinerUPollingExecutor 优雅停机中，等待 active 任务最多 {}s", SHUTDOWN_AWAIT_SECONDS);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("MinerUPollingExecutor 强制停机");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }

    private static ThreadFactory namedFactory() {
        AtomicInteger seq = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, "minerU-poll-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}