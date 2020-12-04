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
 *   PRIMARY KEY (`id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';
 *
 * 其对应的实体是
 * &#047;&#042;&#042; 用户 --> t_user &#042;&#047;
 * &#064;Setter
 * &#064;Getter
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
