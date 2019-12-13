package com.github.global.dynamic;

import java.util.concurrent.ThreadLocalRandom;

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
 * </pre>
 */
public enum ClientDatabase {

    MASTER,
    SLAVE1,
    SLAVE2
    ;

    public static ClientDatabase handleQueryRouter() {
        // 轮询: 申明一个全局 AtomLong, 每次自增 1 并跟从节点的总数(加权则将每个节点的权重相加, 每个节点占一些数字)进行取余
        // 随机: 在从节点的总数内进行随机(加权则将每个节点的权重相加, 每个节点占一些数字)
        // hash: 将用户 ip 与从节点的总数进行取余
        return ThreadLocalRandom.current().nextBoolean() ? SLAVE1 : SLAVE2;
    }
}
