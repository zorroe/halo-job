package com.zorroe.cloud.job.core.util;

import org.quartz.CronExpression;

import java.util.Date;

public class CronUtils {

    /**
     * 判断当前时间是否匹配 cron
     */
    public static boolean isMatch(String cron) {
        try {
            CronExpression cronExpression = new CronExpression(cron);
            return cronExpression.isSatisfiedBy(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
