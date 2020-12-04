package com.github.common.sql;

import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * mp 工具类
 *
 * 比如有 t_table 表
 * CREATE TABLE IF NOT EXISTS `t_user` (
 *   `id` bigint unsigned NOT NULL AUTO_INCREMENT,
 *   `user_name` varchar(32) NOT NULL DEFAULT '' COMMENT '用户名',
 *   `password` varchar(64) NOT NULL DEFAULT '' COMMENT '密码',
 *   `nick_name` varchar(16) NOT NULL DEFAULT '' COMMENT '昵称',
 *   `gender` int NOT NULL DEFAULT '0' COMMENT '性别(0.未知, 1.男, 2.女)',
 *   `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1.已禁用',
 *   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 *   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 *   `is_deleted` bigint NOT NULL DEFAULT '0' COMMENT '非 0 表示已删除, 删除时将当前值置为主键 id 或时间戳即可',
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `user_name` (`user_name`, `is_deleted`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';
 *
 * 其对应的实体是
 * &#047;&#042;&#042; 用户 --> t_user &#042;&#047;
 * &#064;Data
 * &#064;TableName("t_user")
 * public class User {
 *     private Long id;
 *
 *     &#047;&#042;&#042; 用户名 --> user_name &#042;&#047;
 *     private String userName;
 *
 *     &#047;&#042;&#042; 密码 --> password &#042;&#047;
 *     private String password;
 *
 *     &#047;&#042;&#042; 昵称 --> nick_name &#042;&#047;
 *     private String nickName;
 *
 *     &#047;&#042;&#042; 性别(0.未知, 1.男, 2.女) --> gender &#042;&#047;
 *     private Gender gender;
 *
 *     &#047;&#042;&#042; 1.已禁用 --> status &#042;&#047;
 *     private Boolean status;
 *
 *     &#047;&#042;&#042; 创建时间 --> create_time &#042;&#047;
 *     private Date createTime;
 *
 *     &#047;&#042;&#042; 更新时间 --> update_time &#042;&#047;
 *     private Date updateTime;
 *
 *     &#047;&#042;&#042; 非 0 表示已删除, 删除时将当前值置为主键 id 或时间戳即可 --> is_deleted &#042;&#047;
 *     private Long isDeleted;
 * }
 * </pre>
 */
public class MybatisPlusUtil {

    /**
     * java 字段转换成数据库列名, 如: columnToString(User::getUserName) 返回 user_name
     * 
     * @param column lambda 表达式对应的字段, 如 User::getId
     * @param <T> 数据库表对应的实体
     * @return 实体中的属性对应的数据库字段
     */
    public static <T> String fieldToColumn(SFunction<T, ?> column) {
        SerializedLambda lambda = LambdaUtils.resolve(column);
        String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
        Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(lambda.getInstantiatedType());

        String returnColumn;
        if (columnMap != null && columnMap.size() > 0) {
            returnColumn = columnMap.get(LambdaUtils.formatKey(fieldName)).getColumn();
        } else {
            returnColumn = null;
        }

        if (returnColumn == null || returnColumn.trim().length() == 0) {
            // 上面的不成功就简单转换一下: lowerCamel => lower_underscore)
            // 如果是大写(lowerCamel => UPPER_UNDERSCORE)则用 UPPER_UNDERSCORE
            return CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(fieldName);
        } else {
            return returnColumn;
        }
    }

    /**
     * java 字段转换成数据库列名<br><br>
     *
     * 使用 columnsToString(User::getUserName, User::getPassword, User::getNickName)<br>
     * 返回 [user_name, password, nick_name]
     *
     * @param columns lambda 表达式对应的字段数组, 如 [User::getId, User::getUserName]
     * @param <T> 数据库表对应的实体
     * @return 实体中的属性对应的数据库字段
     */
    @SuppressWarnings("unchecked")
    public static <T> String[] fieldsToColumnArray(SFunction<T, ?>... columns) {
        List<String> returnList = Lists.newArrayList();
        for (SFunction<T, ?> column : columns) {
            returnList.add(fieldToColumn(column));
        }
        return returnList.toArray(new String[0]);
    }
}
