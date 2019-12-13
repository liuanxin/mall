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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
public class ClientRouterInterceptor implements Interceptor {

    private static final Map<String, DatabaseRouter> CLASS_METHOD_CACHE = new HashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            if (ms.getSqlCommandType() == SqlCommandType.SELECT) {
                // 比如: com.github.user.repository.UserMapper.selectByExample
                String classAndMethod = ms.getId();

                // 方法或类上有标注解就用标了的
                DatabaseRouter router = getAnnotation(classAndMethod);
                if (router != null) {
                    ClientDatabaseContextHolder.set(router.value());
                } else {
                    boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
                    // 如果不是在一个事务中 且 不是在查询自增主键<select last_insert_id()>则使用从库
                    if (!transactionActive && !classAndMethod.contains(SelectKeyGenerator.SELECT_KEY_SUFFIX)) {
                        ClientDatabaseContextHolder.set(ClientDatabase.handleQueryRouter());
                    }/* else {
                        // 不设置则走默认源
                    }*/
                }
            }

            return invocation.proceed();
        } finally {
            ClientDatabaseContextHolder.clear();
        }
    }

    private DatabaseRouter getAnnotation(String classAndMethod) {
        DatabaseRouter cacheRouter = CLASS_METHOD_CACHE.get(classAndMethod);
        if (cacheRouter != null) {
            return cacheRouter;
        }
        try {
            int endIndex = classAndMethod.lastIndexOf(".");
            String className = classAndMethod.substring(0, endIndex);
            String methodName = classAndMethod.substring(endIndex + 1);

            Class<?> clazz = Class.forName(className);
            if (clazz != null) {
                Method method = clazz.getDeclaredMethod(methodName);
                if (method == null) {
                    method = clazz.getMethod(methodName);
                }

                DatabaseRouter router = null;
                if (method != null) {
                    router = method.getAnnotation(DatabaseRouter.class);
                }
                if (router == null) {
                    router = clazz.getAnnotation(DatabaseRouter.class);
                }
                if (router != null) {
                    CLASS_METHOD_CACHE.put(classAndMethod, router);
                    return router;
                }
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
