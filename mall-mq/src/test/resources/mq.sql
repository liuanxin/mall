
DROP TABLE IF EXISTS `t_mq_send`;
CREATE TABLE IF NOT EXISTS `t_mq_send` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `search_key` varchar(128) NOT NULL DEFAULT '' COMMENT '搜索键',
  `business_type` varchar(32) NOT NULL DEFAULT '' COMMENT '业务场景',
  `status` int unsigned NOT NULL DEFAULT '0' COMMENT '0.初始, 1.失败, 2.成功(需要重试则改为 1)',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '重试次数(需要重试则改为 0)',
  `msg` longtext COMMENT '消息内容',
  `remark` longtext COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_search_key` (`search_key`),
  KEY `idx_status_retry_count` (`status`,`retry_count`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mq 发送消息表';


DROP TABLE IF EXISTS `t_mq_receive`;
CREATE TABLE IF NOT EXISTS `t_mq_receive` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `search_key` varchar(128) NOT NULL DEFAULT '' COMMENT '搜索键',
  `business_type` varchar(32) NOT NULL DEFAULT '' COMMENT '业务场景',
  `status` int unsigned NOT NULL DEFAULT '0' COMMENT '状态(0.初始, 1.失败, 2.成功)',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `msg` longtext COMMENT '消息内容',
  `remark` longtext COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_id` (`msg_id`),
  KEY `idx_search_key` (`search_key`),
  KEY `idx_status_retry_count` (`status`,`retry_count`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mq 消费消息表';
