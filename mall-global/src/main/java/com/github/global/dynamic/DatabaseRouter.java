package com.github.global.dynamic;

import java.lang.annotation.*;

/**
 * <pre>
 * 1. 添加多数据源
 * &#064;Bean
 * public DataSource clientDatasource() {
 *     Map&lt;ClientDatabase, DataSource> targetDataSources = new HashMap<>();
 *     DataSource defaultDatasource = clientADatasource();
 *     DataSource slave1Datasource = clientBDatasource();
 *     DataSource slave2Datasource = clientBDatasource();
 *
 *     targetDataSources.put(ClientDatabase.MASTER, defaultDatasource);
 *     targetDataSources.put(ClientDatabase.SLAVE1, slave1Datasource);
 *     targetDataSources.put(ClientDatabase.SLAVE2, slave2Datasource);
 *
 *     return new ClientDataSourceRouter(targetDataSources, defaultDatasource);
 * }
 *
 * 2. 添加 mybatis 插件
 * sessionFactory.setPlugins(new MybatisQueryInterceptor());
 *
 * 之后所有的查询都会走自动切换, 如果 Mapper 类或方法上有标 @DatabaseRouter 注解则使用相应的数据源
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
