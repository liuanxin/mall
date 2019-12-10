package com.github.global.dynamic;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
@Intercepts({
    @Signature(
        type = Executor.class, method = "query",
        args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class }
    ),
    @Signature(
        type = Executor.class, method = "query",
        args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class }
    )
})
public class MybatisQueryInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            boolean hasTransaction = TransactionSynchronizationManager.isActualTransactionActive();
            // 查询自增主键: select last_insert_id()
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            boolean hasSelectKey = mappedStatement.getSqlCommandType().equals(SqlCommandType.SELECT)
                    && mappedStatement.getId().contains(SelectKeyGenerator.SELECT_KEY_SUFFIX);

            // 如果是在一个事务或者在查询自增主键则使用主库
            ClientDatabase clientDatabase;
            if (hasTransaction || hasSelectKey) {
                clientDatabase = ClientDatabase.MASTER;
            } else {
                // 看方法或类上有没有标注解, 没有标就随机用一个从库, 有标就用标了的
                DatabaseRouter router = invocation.getMethod().getAnnotation(DatabaseRouter.class);
                if (router == null) {
                    router = invocation.getMethod().getReturnType().getAnnotation(DatabaseRouter.class);
                }
                clientDatabase = (router == null ? ClientDatabase.randomSlave() : router.value());
            }
            ClientDatabaseContextHolder.set(clientDatabase);

            return invocation.proceed();
        } finally {
            ClientDatabaseContextHolder.clear();
        }
    }
}
