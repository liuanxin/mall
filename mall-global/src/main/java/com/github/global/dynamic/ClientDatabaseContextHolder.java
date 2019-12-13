package com.github.global.dynamic;

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
 * 这之后基于 mybatis 做的 select 查询:
 *   如果 Mapper 类或方法上有标 @DatabaseRouter 注解则使用相应的数据源
 *   如果未标注解, 则: 如果不是在一个事务中 且 不是在查询自增主键: select last_insert_id() 则使用从库
 *     规则见: {@link ClientDatabase#handleQueryRouter()}
 *
 * 默认使用上面的 defaultDataSource
 * </pre>
 */
public class ClientDatabaseContextHolder {

    private static final ThreadLocal<ClientDatabase> CONTEXT = new ThreadLocal<>();

    public static void set(ClientDatabase clientDatabase) {
        CONTEXT.set(clientDatabase);
    }

    public static ClientDatabase get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
