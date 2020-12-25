//package com.github.common.sql;
//
//import com.github.common.date.DateUtil;
//import com.github.common.util.LogUtil;
//import com.github.common.util.U;
//import com.google.common.cache.Cache;
//import com.google.common.cache.CacheBuilder;
//import com.mysql.jdbc.Connection;
//import com.mysql.jdbc.ResultSetInternalMethods;
//import com.mysql.jdbc.Statement;
//import com.mysql.jdbc.StatementInterceptor;
//
//import java.sql.SQLException;
//import java.util.Properties;
//import java.util.concurrent.TimeUnit;
//
///**
// * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
// * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
// */
//public class ShowSql5Interceptor implements StatementInterceptor {
//
//    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
//    private static final Cache<Thread, Long> TIME_CACHE = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();
//
//    @Override
//    public void init(Connection connection, Properties properties) throws SQLException {}
//
//    @Override
//    public ResultSetInternalMethods preProcess(String sql, Statement statement,
//                                               Connection connection) throws SQLException {
//        TIME_CACHE.put(Thread.currentThread(), System.currentTimeMillis());
//        return null;
//    }
//
//    @Override
//    public ResultSetInternalMethods postProcess(String sql, Statement statement, ResultSetInternalMethods resultSet,
//                                                Connection connection) throws SQLException {
//        Thread thread = Thread.currentThread();
//        try {
//            if (U.isBlank(sql) && U.isNotBlank(statement)) {
//                sql = statement.toString();
//                if (U.isNotBlank(sql)) {
//                    int i = sql.indexOf(':');
//                    if (i > 0 ) {
//                        sql = sql.substring(i + 1).trim();
//                    }
//                }
//            }
//            if (U.isNotBlank(sql)) {
//                if (LogUtil.SQL_LOG.isDebugEnabled()) {
//                    StringBuilder sbd = new StringBuilder();
//
//                    Long start = TIME_CACHE.getIfPresent(thread);
//                    if (U.greater0(start)) {
//                        sbd.append("time: ").append(DateUtil.toHuman(System.currentTimeMillis() - start)).append(" ms, ");
//                    }
//                    if (resultSet != null && resultSet.reallyResult() && resultSet.last()) {
//                        int size = resultSet.getRow();
//                        resultSet.beforeFirst();
//                        if (size > 0) {
//                            sbd.append("size: ").append(size).append(", ");
//                        }
//                    }
//                    // druid -> SQLUtils.formatMySql
//                    sbd.append("sql:\n").append(SqlFormat.format(sql).replaceFirst("^\\s*?\n", ""));
//
//                    LogUtil.SQL_LOG.debug(sbd.toString());
//                }
//            }
//        } finally {
//            TIME_CACHE.invalidate(thread);
//        }
//        return null;
//    }
//
//    @Override
//    public boolean executeTopLevelOnly() { return false; }
//
//    @Override
//    public void destroy() {
//        TIME_CACHE.invalidateAll();
//    }
//}
