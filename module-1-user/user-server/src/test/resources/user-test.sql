
DROP TABLE IF EXISTS `t_user_test`;
CREATE TABLE IF NOT EXISTS `t_user_test` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_name` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '用户名',
    `password` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '密码',
    `level` TINYINT(4) UNSIGNED NOT NULL DEFAULT '0' COMMENT '用户等级(0.普通, 1.vip)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';


DROP TABLE IF EXISTS `t_user_test_extend`;
CREATE TABLE IF NOT EXISTS `t_user_test_extend` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL DEFAULT '0',
    `nick_name` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '用户昵称',
    `gender` TINYINT(4) UNSIGNED NOT NULL DEFAULT '0' COMMENT '性别(0.未知, 1.男, 2.女)',
    `birthday` CHAR(4) NOT NULL DEFAULT '' COMMENT '用户生日(0102 表示 1月2日)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';


INSERT INTO `t_user_test`(`id`, `user_name`, `password`) VALUES(1, 'zhangsan', '123'), (2, 'lisi', 'abc');

INSERT INTO `t_user_test_extend`(`user_id`, `nick_name`, `gender`, `birthday`)
VALUES(1, '张三', 1, '1010'), (2, '李四', 2, '1212');
