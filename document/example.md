
在本地建下面两个表
```sql
DROP DATABASE IF EXISTS `mall`;
CREATE DATABASE IF NOT EXISTS `mall` DEFAULT CHARACTER SET utf8;
USE `mall`;

DROP TABLE IF EXISTS `t_user_test`;
CREATE TABLE IF NOT EXISTS `t_user_test` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `nick_name` varchar(32) NOT NULL DEFAULT '' COMMENT '昵称',
  `gender` int(11) NOT NULL DEFAULT '0' COMMENT '性别',
  `level` INT(11) NOT NULL DEFAULT '0' COMMENT '等级',
  `avatar_url` varchar(64) NOT NULL DEFAULT '' COMMENT '头像',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户示例';

INSERT INTO `t_user_test` (`id`, `nick_name`, `gender`, `avatar_url`) VALUES
  (1, '张一', 1, '//abcdp0.png'),
  (2, '张二', 1, '//abcde3.png'),
  (3, '张三', 2, '//abcdef2.png'),
  (4, '张四', 2, '//abcdefg.png'),
  (5, '张五', 1, '//abcdfho.png'),
  (6, '张六', 1, '//abcdeg1.png'),
  (7, '李一', 2, '//abcdhfd.png'),
  (8, '李二', 1, '//abcdifd.png'),
  (9, '李三', 0, '//abcdgre.png'),
  (10, '李四', 2, '//abcdqh1.png'),
  (11, '李五', 0, '//abcdkre.png'),
  (12, '李六', 1, '//abcdore.png');


DROP TABLE IF EXISTS `t_product_test`;
CREATE TABLE IF NOT EXISTS `t_product_test` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '所属用户',
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT '商品名(60 个字以内)',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '商品类型',
  `create_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '最近更新时间',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 表示已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品示例';

INSERT INTO `t_product_test` (`id`, `user_id`, `name`, `type`, `create_time`, `update_time`) VALUES
  (1, 1, 'iphone', 1, '2018-01-01 16:38:03', '2018-02-01 16:39:09'),
  (2, 2, 'sunsong', 1, '2018-01-01 16:38:11', '0000-00-00 00:00:00'),
  (3, 2, 'Nokia', 2, '2018-01-09 16:38:27', '0000-00-00 00:00:00'),
  (4, 8, 'LG', 2, '2018-01-11 16:39:52', '2018-07-11 16:40:29'),
  (5, 4, 'mi', 2, '2018-01-12 16:38:27', '0000-00-00 00:00:00'),
  (6, 2, 'smartisan', 2, '2018-02-14 16:39:52', '2018-05-10 16:40:29'),
  (7, 1, 'oppo', 2, '2018-02-12 16:38:27', '0000-00-00 00:00:00'),
  (8, 5, 'vivo', 2, '2018-03-10 16:39:52', '2018-03-18 16:40:29'),
  (9, 2, '1 plus', 2, '2018-02-11 16:38:27', '0000-00-00 00:00:00'),
  (10, 2, 'huawei', 2, '2018-03-17 16:39:52', '2018-03-19 16:40:29');
```

在本地 C:/Windows/System32/drivers/etc/hosts 文件中添加下面的配置
```text
127.0.0.1  dev-db
# 127.0.0.1  dev-redis
```

运行 `WebBackendApplication.java`, 请求 `http://127.0.0.1:8686/example ?gender=0|1|2` 查看示例

请求后端: http://127.0.0.1:8686/static/api-info-example.html  
项目文档: http://127.0.0.1:8686/static/api-info.html  
在商品模块的接口处, 向后端发起请求, 会打印出 sql 和 请求的整个上下文及返回

~
