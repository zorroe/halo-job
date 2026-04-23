package com.zorroe.cloud.job.core.component;

import com.zorroe.cloud.job.core.common.BlockStrategyEnum;
import com.zorroe.cloud.job.core.model.TriggerJobRequest;
import com.zorroe.cloud.job.core.model.TriggerJobResponse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorJobRunner {

    private static final int DEFAULT_QUEUE_CAPACITY = 100;

    private final ConcurrentMap<String, ExecutionSlot> slots = new ConcurrentHashMap<>();
    private final AtomicInteger workerIndex = new AtomicInteger(1);

    /**
     * 按任务维度分配独立执行槽位，并依据阻塞策略提交任务。
     *
     * @param request 任务触发请求
     * @param task 具体执行逻辑
     * @return 执行结果
     */
    public TriggerJobResponse run(TriggerJobRequest request, Callable<TriggerJobResponse> task) {
        String jobKey = buildJobKey(request);
        ExecutionSlot slot = slots.computeIfAbsent(jobKey, key -> new ExecutionSlot(workerIndex.getAndIncrement()));
        return slot.submit(request, task);
    }

    /**
     * 为触发请求生成稳定的任务键，用于隔离不同任务的串行执行队列。
     *
     * @param request 任务触发请求
     * @return 任务键
     */
    private String buildJobKey(TriggerJobRequest request) {
        if (request != null && request.getJobId() != null) {
            return "job:" + request.getJobId();
        }
        return "handler:" + (request == null ? "unknown" : request.getHandler());
    }

    private static final class ExecutionSlot {

        private final Object monitor = new Object();
        private final Deque<QueueTask> waitingQueue = new ArrayDeque<>();
        private final Thread workerThread;
        private volatile QueueTask currentTask;

        /**
         * 创建执行槽位并立即启动专属工作线程。
         *
         * @param index 工作线程编号
         */
        private ExecutionSlot(int index) {
            this.workerThread = new Thread(this::workerLoop, "halo-job-runner-" + index);
            this.workerThread.setDaemon(true);
            this.workerThread.start();
        }

        /**
         * 根据阻塞策略把任务放入当前槽位的等待队列，并同步等待执行结果。
         *
         * @param request 任务触发请求
         * @param callable 具体执行逻辑
         * @return 执行结果
         */
        private TriggerJobResponse submit(TriggerJobRequest request, Callable<TriggerJobResponse> callable) {
            QueueTask queueTask = new QueueTask(request, callable);
            BlockStrategyEnum strategy = BlockStrategyEnum.getByCode(request == null ? null : request.getBlockStrategy());

            synchronized (monitor) {
                boolean busy = currentTask != null || !waitingQueue.isEmpty();
                switch (strategy) {
                    case DISCARD_NEW -> {
                        if (busy) {
                            return TriggerJobResponse.discarded(request, "当前任务正在执行，已丢弃新触发", 0L);
                        }
                        waitingQueue.addLast(queueTask);
                    }
                    case COVER_RUNNING -> {
                        if (currentTask != null) {
                            currentTask.cancel("当前任务被新的触发请求覆盖");
                        }
                        clearWaitingQueue("队列中的旧任务被新的触发请求覆盖");
                        waitingQueue.addFirst(queueTask);
                    }
                    case QUEUE_WAIT -> {
                        if (waitingQueue.size() >= DEFAULT_QUEUE_CAPACITY) {
                            return TriggerJobResponse.discarded(request, "等待队列已满，无法继续排队", 0L);
                        }
                        waitingQueue.addLast(queueTask);
                    }
                }
                monitor.notifyAll();
            }

            return queueTask.await();
        }

        /**
         * 清空等待中的旧任务，并统一标记为被新的触发请求覆盖。
         *
         * @param reason 丢弃原因
         */
        private void clearWaitingQueue(String reason) {
            QueueTask task;
            while ((task = waitingQueue.pollFirst()) != null) {
                task.complete(TriggerJobResponse.discarded(task.request, reason, 0L));
            }
        }

        /**
         * 工作线程主循环，持续消费队列中的任务并保持同一任务键串行执行。
         */
        private void workerLoop() {
            while (true) {
                QueueTask task;
                synchronized (monitor) {
                    while (waitingQueue.isEmpty()) {
                        currentTask = null;
                        try {
                            monitor.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                    task = waitingQueue.pollFirst();
                    currentTask = task;
                    task.markRunning(Thread.currentThread());
                }

                task.run();

                synchronized (monitor) {
                    if (currentTask == task) {
                        currentTask = null;
                    }
                }
            }
        }
    }

    private static final class QueueTask {

        private final TriggerJobRequest request;
        private final Callable<TriggerJobResponse> callable;
        private final CompletableFuture<TriggerJobResponse> future = new CompletableFuture<>();
        private volatile Thread runningThread;
        private volatile boolean canceled;
        private volatile String cancelReason;

        /**
         * 封装一次排队中的任务调用请求。
         *
         * @param request 任务触发请求
         * @param callable 实际调用逻辑
         */
        private QueueTask(TriggerJobRequest request, Callable<TriggerJobResponse> callable) {
            this.request = request;
            this.callable = callable;
        }

        /**
         * 在任务真正开始执行时记录执行线程，便于覆盖策略中断正在运行的任务。
         *
         * @param thread 当前执行线程
         */
        private void markRunning(Thread thread) {
            this.runningThread = thread;
        }

        /**
         * 标记任务已取消，并尽量中断正在执行的线程。
         *
         * @param reason 取消原因
         */
        private void cancel(String reason) {
            this.canceled = true;
            this.cancelReason = reason;
            Thread thread = this.runningThread;
            if (thread != null) {
                thread.interrupt();
            }
        }

        /**
         * 执行实际任务调用，并把异常、中断和取消情况统一转换为标准响应。
         */
        private void run() {
            try {
                if (future.isDone()) {
                    return;
                }
                if (canceled) {
                    complete(TriggerJobResponse.canceled(request, defaultCancelReason(), 0L));
                    return;
                }

                TriggerJobResponse response = callable.call();
                if (future.isDone()) {
                    return;
                }

                if (response == null) {
                    response = TriggerJobResponse.failed(request, "任务执行结果为空", 0L);
                }

                if (canceled || Thread.currentThread().isInterrupted()) {
                    complete(TriggerJobResponse.canceled(request, defaultCancelReason(), response.getCostTime() == null ? 0L : response.getCostTime()));
                    return;
                }

                complete(response);
            } catch (Exception e) {
                if (future.isDone()) {
                    return;
                }

                if (canceled || Thread.currentThread().isInterrupted()) {
                    complete(TriggerJobResponse.canceled(request, defaultCancelReason(), 0L));
                    return;
                }

                complete(TriggerJobResponse.failed(request, resolveMessage(e), 0L));
            } finally {
                Thread.interrupted();
            }
        }

        /**
         * 等待任务执行完成并返回统一结果。
         *
         * @return 执行结果
         */
        private TriggerJobResponse await() {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return TriggerJobResponse.canceled(request, "等待任务结果时线程被中断", 0L);
            } catch (ExecutionException e) {
                return TriggerJobResponse.failed(request, resolveMessage(e.getCause()), 0L);
            }
        }

        /**
         * 完成任务结果通知，唤醒等待中的调用方。
         *
         * @param response 执行结果
         */
        private void complete(TriggerJobResponse response) {
            future.complete(response);
        }

        /**
         * 获取兜底取消原因，保证被取消的任务总有明确反馈。
         *
         * @return 取消原因文案
         */
        private String defaultCancelReason() {
            return cancelReason == null || cancelReason.isBlank() ? "任务已取消" : cancelReason;
        }

        /**
         * 从异常链末端提取最适合返回给调度方的错误信息。
         *
         * @param throwable 原始异常
         * @return 可读错误信息
         */
        private String resolveMessage(Throwable throwable) {
            Throwable target = throwable == null ? null : throwable;
            while (target != null && target.getCause() != null && target.getCause() != target) {
                target = target.getCause();
            }
            if (target == null) {
                return "任务执行失败";
            }
            if (target.getMessage() != null && !target.getMessage().isBlank()) {
                return target.getMessage();
            }
            return target.getClass().getSimpleName();
        }
    }
}
