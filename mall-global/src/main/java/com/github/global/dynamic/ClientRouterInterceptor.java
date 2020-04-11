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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 *     规则见: {@link ClientDatabase#handleSlaveRouter()}
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

    private static final Map<String, Object> CLASS_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Object NIL_OBJ = new Object();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            // 只在查询时路由
            if (ms.getSqlCommandType() == SqlCommandType.SELECT) {
                // 比如: com.github.user.repository.UserMapper.selectByExample
                String classAndMethod = ms.getId();

                ClientDatabase clientData;
                boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
                // 如果是在一个事务中 或者 是在查询自增主键<select last_insert_id()> 则强制走主库
                if (transactionActive || classAndMethod.endsWith(SelectKeyGenerator.SELECT_KEY_SUFFIX)) {
                    clientData = ClientDatabase.handleMasterRouter();
                } else {
                    // 方法或类上有标注解就用标了的, 否则使用从库
                    DatabaseRouter router = getAnnotation(classAndMethod);
                    clientData = (router != null ? router.value() : ClientDatabase.handleSlaveRouter());
                }
                ClientDatabaseContextHolder.set(clientData);
            }
            return invocation.proceed();
        } finally {
            ClientDatabaseContextHolder.clear();
        }
    }

    private DatabaseRouter getAnnotation(String classAndMethod) {
        Object cacheRouter = CLASS_METHOD_CACHE.get(classAndMethod);
        // 如果在缓存里的是一个空对象, 直接返回 null
        if (cacheRouter == NIL_OBJ) {
            return null;
        }
        if (cacheRouter instanceof DatabaseRouter) {
            return (DatabaseRouter) cacheRouter;
        }

        // 比如: com.github.user.repository.UserMapper.selectByExample
        int endIndex = classAndMethod.lastIndexOf(".");
        String className = classAndMethod.substring(0, endIndex);
        String methodName = classAndMethod.substring(endIndex + 1);

        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }

        Method method;
        try {
            method = clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException | SecurityException e) {
            method = null;
        }
        if (method == null) {
            try {
                method = clazz.getMethod(methodName);
            } catch (NoSuchMethodException | SecurityException ignore) {
            }
        }

        DatabaseRouter router = null;
        if (method != null) {
            router = method.getAnnotation(DatabaseRouter.class);
        }
        if (router == null) {
            router = clazz.getAnnotation(DatabaseRouter.class);
        }

        // 方法或类上都没有标注解就拿一个空对象写入缓存, 这样后面再来的时候在最上面就可以直接返回了
        CLASS_METHOD_CACHE.put(classAndMethod, (router == null ? NIL_OBJ : router));
        return router;
    }
}
