package com.zorroe.cloud.job.core.util;

import org.quartz.CronExpression;

import java.util.Date;

public class CronUtils {

    /**
     * 判断当前时间是否匹配 cron 表达式
     *
     * @param cron Cron 表达式
     * @return 是否匹配
     */
    public static boolean isMatch(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return false;
        }

        try {
            CronExpression cronExpression = new CronExpression(cron);
            return cronExpression.isSatisfiedBy(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取下一个符合 Cron 表达式的执行时间
     *
     * @param cron Cron 表达式
     * @param fromDate 起始时间
     * @return 下一个执行时间
     */
    public static Date getNextFireTime(String cron, Date fromDate) {
        if (cron == null || cron.trim().isEmpty() || fromDate == null) {
            return null;
        }

        try {
            CronExpression cronExpression = new CronExpression(cron);
            return cronExpression.getNextValidTimeAfter(fromDate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Cron 表达式是否合法
     *
     * @param cron Cron 表达式
     * @return 是否合法
     */
    public static boolean isValid(String cron) {
        if (cron == null || cron.trim().isEmpty()) {
            return false;
        }

        try {
            new CronExpression(cron);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ✅ 新增：获取下一个执行时间（毫秒时间戳）
     */
    public static Long getNextFireTimeMillis(String cron, Date fromDate) {
        if (cron == null || cron.trim().isEmpty() || fromDate == null) {
            return null;
        }

        try {
            CronExpression cronExpression = new CronExpression(cron);
            Date nextTime = cronExpression.getNextValidTimeAfter(fromDate);
            return nextTime != null ? nextTime.getTime() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
