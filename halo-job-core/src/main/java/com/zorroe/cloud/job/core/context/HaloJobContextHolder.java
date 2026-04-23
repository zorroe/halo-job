package com.zorroe.cloud.job.core.context;

public final class HaloJobContextHolder {

    private static final ThreadLocal<HaloJobContext> CONTEXT = new ThreadLocal<>();

    private HaloJobContextHolder() {
    }

    public static void set(HaloJobContext context) {
        CONTEXT.set(context);
    }

    public static HaloJobContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static int getShardIndex() {
        HaloJobContext context = get();
        return context == null || context.getShardIndex() == null ? 0 : context.getShardIndex();
    }

    public static int getShardTotal() {
        HaloJobContext context = get();
        return context == null || context.getShardTotal() == null ? 1 : context.getShardTotal();
    }
}
