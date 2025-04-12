
在本地建下面两个表
```sql
DROP DATABASE IF EXISTS `mall`;
CREATE DATABASE IF NOT EXISTS `mall` DEFAULT CHARACTER SET utf8;
USE `mall`;

DROP TABLE IF EXISTS `t_user_test`;
CREATE TABLE IF NOT EXISTS `t_user_test` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nick_name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '昵称',
  `gender` INT NOT NULL DEFAULT '0' COMMENT '性别',
  `level` INT NOT NULL DEFAULT '0' COMMENT '等级',
  `avatar_url` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '头像',
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
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT '所属用户',
  `name` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '商品名(60 个字以内)',
  `type` INT NOT NULL DEFAULT '0' COMMENT '商品类型',
  `create_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近更新时间',
  `is_delete` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '1 表示已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品示例';

INSERT INTO `t_product_test` (`id`, `user_id`, `name`, `type`) VALUES
  (1, 1, 'iphone', 1),
  (2, 2, 'sunsong', 1),
  (3, 2, 'Nokia', 2),
  (4, 8, 'LG', 2),
  (5, 4, 'mi', 2),
  (6, 2, 'smartisan', 2),
  (7, 1, 'oppo', 2),
  (8, 5, 'vivo', 2),
  (9, 2, '1 plus', 2),
  (10, 2, 'huawei', 2);
```

在本地 C:/Windows/System32/drivers/etc/hosts 文件中添加下面的配置
```text
127.0.0.1  dev-db
# 127.0.0.1  dev-redis
```

运行 `WebBackendApplication.java`, 请求 `http://127.0.0.1:8686/example?gender=0|1|2` 查看示例

请求后端: http://127.0.0.1:8686/static/api-info-example.html  
项目文档: http://127.0.0.1:8686/static/api-info.html  
在商品模块的接口处, 向后端发起请求, 会打印出 sql 和 请求的整个上下文及返回

~
