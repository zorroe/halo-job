-- Halo-Job database initialization script
-- Compatible with MySQL 5.7+ / 8.x

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `halo-job`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `halo-job`;

-- ------------------------------------
-- Table structure for job_info
-- ------------------------------------
CREATE TABLE IF NOT EXISTS `job_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `job_name` VARCHAR(100) NOT NULL COMMENT 'Job name',
    `executor_group` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Executor group',
    `executor_handler` VARCHAR(100) NOT NULL COMMENT 'Executor handler name',
    `executor_app` VARCHAR(100) NOT NULL COMMENT 'Executor app name',
    `executor_param` VARCHAR(1000) DEFAULT NULL COMMENT 'Executor parameter',
    `job_status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Job status: 0-stop, 1-run',
    `trigger_type` VARCHAR(32) DEFAULT NULL COMMENT 'Trigger type: CRON/ONCE/FIXED_RATE/FIXED_DELAY',
    `trigger_config` TEXT DEFAULT NULL COMMENT 'Trigger config json',
    `next_execute_time` BIGINT DEFAULT NULL COMMENT 'Next execute time in milliseconds',
    `owner` VARCHAR(64) DEFAULT NULL COMMENT 'Owner',
    `tag` VARCHAR(128) DEFAULT NULL COMMENT 'Tag',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT 'Remark',
    `route_strategy` TINYINT NOT NULL DEFAULT 1 COMMENT 'Route strategy: 1-round, 2-random, 3-first, 4-last, 5-hash, 6-sharding_broadcast',
    `block_strategy` TINYINT NOT NULL DEFAULT 1 COMMENT 'Block strategy: 1-queue_wait, 2-discard_new, 3-cover_running',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT 'Retry count',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    KEY `idx_job_status_next_execute_time` (`job_status`, `next_execute_time`),
    KEY `idx_job_group_handler` (`executor_group`, `executor_handler`),
    KEY `idx_job_app` (`executor_app`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'Job definition table';

-- ------------------------------------
-- Table structure for executor_info
-- ------------------------------------
CREATE TABLE IF NOT EXISTS `executor_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `executor_name` VARCHAR(100) NOT NULL COMMENT 'Executor name',
    `executor_address` VARCHAR(255) NOT NULL COMMENT 'Executor address',
    `executor_group` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Executor group',
    `executor_app` VARCHAR(100) NOT NULL COMMENT 'Executor app',
    `metadata` TEXT DEFAULT NULL COMMENT 'Executor metadata json',
    `version` VARCHAR(64) DEFAULT NULL COMMENT 'Executor version',
    `heartbeat_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last heartbeat time',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Executor status: 0-offline, 1-online',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_executor_address` (`executor_address`),
    KEY `idx_executor_status_heartbeat` (`status`, `heartbeat_time`),
    KEY `idx_executor_group_app` (`executor_group`, `executor_app`),
    KEY `idx_executor_name` (`executor_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'Executor registration table';

-- ------------------------------------
-- Table structure for executor_handler
-- ------------------------------------
CREATE TABLE IF NOT EXISTS `executor_handler` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `executor_name` VARCHAR(100) NOT NULL COMMENT 'Executor name',
    `executor_address` VARCHAR(255) NOT NULL COMMENT 'Executor address',
    `executor_group` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT 'Executor group',
    `executor_app` VARCHAR(100) NOT NULL COMMENT 'Executor app',
    `handler_name` VARCHAR(100) NOT NULL COMMENT 'Handler name',
    `handler_desc` VARCHAR(255) DEFAULT NULL COMMENT 'Handler description',
    `method_signature` VARCHAR(255) DEFAULT NULL COMMENT 'Method signature',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_executor_handler` (`executor_address`, `handler_name`),
    KEY `idx_handler_group_app` (`executor_group`, `executor_app`, `handler_name`),
    KEY `idx_handler_name` (`handler_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'Executor handler registry table';

-- ------------------------------------
-- Table structure for job_execution_log
-- ------------------------------------
CREATE TABLE IF NOT EXISTS `job_execution_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `job_id` BIGINT NOT NULL COMMENT 'Job id',
    `job_name` VARCHAR(100) NOT NULL COMMENT 'Job name',
    `executor_handler` VARCHAR(100) NOT NULL COMMENT 'Executor handler name',
    `executor_param` VARCHAR(1000) DEFAULT NULL COMMENT 'Executor parameter',
    `status` TINYINT NOT NULL COMMENT 'Execute status: 0-fail, 1-success',
    `error_msg` VARCHAR(2000) DEFAULT NULL COMMENT 'Error message',
    `execution_time` BIGINT DEFAULT NULL COMMENT 'Execution time in milliseconds',
    `start_time` DATETIME NOT NULL COMMENT 'Start time',
    `end_time` DATETIME NOT NULL COMMENT 'End time',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    PRIMARY KEY (`id`),
    KEY `idx_job_id_start_time` (`job_id`, `start_time`),
    KEY `idx_start_time` (`start_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = 'Job execution log table';

SET FOREIGN_KEY_CHECKS = 1;

-- Sample seed data
INSERT INTO `job_info` (
    `job_name`,
    `executor_group`,
    `executor_handler`,
    `executor_app`,
    `executor_param`,
    `job_status`,
    `trigger_type`,
    `trigger_config`,
    `next_execute_time`,
    `remark`,
    `route_strategy`,
    `block_strategy`,
    `retry_count`
)
SELECT
    'Demo data sync job',
    'default',
    'dataSyncTask',
    'halo-job-executor',
    'demo-param',
    1,
    'FIXED_RATE',
    '{"intervalSeconds":300}',
    UNIX_TIMESTAMP(NOW()) * 1000 + 300000,
    'Seed job created by init script',
    1,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1
    FROM `job_info`
    WHERE `executor_group` = 'default'
      AND `executor_handler` = 'dataSyncTask'
);
