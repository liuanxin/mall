package com.github.global.dynamic;

import java.lang.annotation.*;

/**
 * <pre>
 * 1. 添加多数据源
 * &#064;Bean
 * public DataSource clientDataSource() {
 *     Map&lt;ClientDatabase, DataSource> targetDataSources = new HashMap<>();
 *     DataSource defaultDataSource = clientADataSource();
 *     DataSource slave1DataSource = clientBDataSource();
 *     DataSource slave2DataSource = clientCDataSource();
 *
 *     targetDataSources.put(ClientDatabase.MASTER, defaultDataSource);
 *     targetDataSources.put(ClientDatabase.SLAVE1, slave1DataSource);
 *     targetDataSources.put(ClientDatabase.SLAVE2, slave2DataSource);
 *
 *     return new ClientDataSourceRouter(defaultDataSource, targetDataSources);
 * }
 *
 * 2. 添加 mybatis 插件
 * sessionFactory.setPlugins(new ClientRouterInterceptor());
 *
 * 如果 Mapper 类或方法上有标 @DatabaseRouter 注解则使用相应的数据源
 * 如果未标注解, 则: 如果不是在一个事务中 且 不是在查询自增主键: select last_insert_id() 则使用从库
 *
 * 默认使用上面的 defaultDataSource
 *
 * PS: 如果查询是在事务中, 又或者在查询自增主键(类似 select last_insert_id() 这种), 则标了 @DatabaseRouter 也走主库
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DatabaseRouter {

    ClientDatabase value() default ClientDatabase.MASTER;
}
