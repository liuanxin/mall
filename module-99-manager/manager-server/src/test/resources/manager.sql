
/*
用户(user)、角色(role)、菜单(menu)、权限(permission), 如果粒度要再细一点, 可以有按钮(button)

用户(user) 与 角色(role) 是多对多的关系
角色(role) 与 菜单(menu) 是多对多的关系
角色(role) 与 权限(permission) 是多对多的关系
菜单(menu) 与 权限(permission) 是一对多的关系

PS: 要实现多对多需要中间表, 一对多只需要在 <多的那个表> 中有 <一的那个表的 id> 即可
菜单(menu) 与 权限(permission) 也可以是多对多的关系, 没必要有那个复杂度, 只在 <权限表> 中有 <菜单 id> 就好了
后台可以理解成一个 Controller 就是一个菜单(menu), 里面一个方法就是一个权限(permission)

用户登录后将所有角色底下的菜单和权限都合并起来, 菜单按层级关系返回给前端(如果到按钮级也返回), 权限存入后端 session 做访问控制(method + url)
PS: 好的用户体验是管理员可以设置: 合并角色或弹出下拉让用户选择角色后登入, 默认合并, 没必要有那个复杂度, 当前只精简合并就好了

菜单(menu)需要跟前端一一对应, 前端每加一条需要后端管理的菜单, 后台也要有(用 path 或 name 来对应)
PS: 不需要后端管理的菜单后端不需要, 比如每个人都可以操作的修改密码这种

前端获取到层级关系的菜单(menu)后动态显示(如果用户数据中有说明是管理员则无视菜单全部显示)
*/

DROP TABLE IF EXISTS `t_manager_user`;
CREATE TABLE IF NOT EXISTS `t_manager_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '用户名',
  `password` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '密码',
  `nick_name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(256) NOT NULL DEFAULT '' COMMENT '头像',
  `has_manager` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0' COMMENT '1 表示是管理员',
  `status` TINYINT(1) UNSIGNED NOT NULL DEFAULT '0' COMMENT '1 表示已禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_name` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';


DROP TABLE IF EXISTS `t_manager_role`;
CREATE TABLE IF NOT EXISTS `t_manager_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '角色名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色, 与 用户是 多对多 的关系';


DROP TABLE IF EXISTS `t_manager_menu`;
CREATE TABLE IF NOT EXISTS `t_manager_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT UNSIGNED NOT NULL COMMENT '父菜单, 0 则表示是根菜单',
  `name` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '菜单说明',
  `front` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '前端对应的值(如 path 或 name)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单, 需要跟前端对应, 前端每增加一个菜单就需要添加一条记录, 与角色是 多对多 的关系';


DROP TABLE IF EXISTS `t_manager_permission`;
CREATE TABLE IF NOT EXISTS `t_manager_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '所属菜单',
  `name` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '权限说明, 如(查询用户)',
  `method` VARCHAR(8) NOT NULL DEFAULT '' COMMENT 'GET 或 POST 等, * 表示通配',
  `url` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '如 /user',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `method_url` (`method`, `url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限, 对应每一个后台请求, 跟菜单是 多对一 的关系, 跟角色是 多对多 的关系';

/* 下面是关联表 */

DROP TABLE IF EXISTS `t_manager_user_role`;
CREATE TABLE IF NOT EXISTS `t_manager_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uid_rid` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色的中间表';


DROP TABLE IF EXISTS `t_manager_role_menu`;
CREATE TABLE IF NOT EXISTS `t_manager_role_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 id',
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `rid_mid` (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单的中间表';


DROP TABLE IF EXISTS `t_manager_role_permission`;
CREATE TABLE IF NOT EXISTS `t_manager_role_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色 id',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限 id',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `rid_pid` (`role_id`, `permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和权限的中间表';
