package com.zorroe.cloud.job.core.context;

public final class HaloJobContextHolder {

    private static final ThreadLocal<HaloJobContext> CONTEXT = new ThreadLocal<>();

    private HaloJobContextHolder() {
    }

    /**
     * 为当前执行线程绑定任务上下文。
     *
     * @param context 任务上下文
     */
    public static void set(HaloJobContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程绑定的任务上下文。
     *
     * @return 任务上下文
     */
    public static HaloJobContext get() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程中的任务上下文，避免线程复用造成脏数据泄漏。
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前任务的分片序号，未设置时返回默认值 0。
     *
     * @return 当前分片序号
     */
    public static int getShardIndex() {
        HaloJobContext context = get();
        return context == null || context.getShardIndex() == null ? 0 : context.getShardIndex();
    }

    /**
     * 获取当前任务的总分片数，未设置时返回默认值 1。
     *
     * @return 分片总数
     */
    public static int getShardTotal() {
        HaloJobContext context = get();
        return context == null || context.getShardTotal() == null ? 1 : context.getShardTotal();
    }
}
