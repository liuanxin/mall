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
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
// * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
// */
//public class ShowSql5Interceptor implements StatementInterceptor {
//
//    private static final String TIME_SPLIT = "~";
//    private static final AtomicLong COUNTER = new AtomicLong(0L);
//    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
//    private static final Cache<Thread, String> TIME_CACHE = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
//
//    @Override
//    public void init(Connection connection, Properties properties) throws SQLException {}
//
//    @Override
//    public ResultSetInternalMethods preProcess(String sql, Statement statement, Connection connection) throws SQLException {
//        if (LogUtil.SQL_LOG.isDebugEnabled()) {
//            String realSql = getRealSql(sql, statement);
//            if (U.isNotBlank(realSql)) {
//                Thread currentThread = Thread.currentThread();
//                long counter = COUNTER.addAndGet(1);
//                long start = System.currentTimeMillis();
//
//                TIME_CACHE.put(currentThread, counter + TIME_SPLIT + start);
//                String url = connection.getMetaData().getURL();
//                LogUtil.SQL_LOG.debug("counter: {}, ip:port/db:({}), sql: {}", counter,
//                        url.substring(url.indexOf("//") + 2, url.indexOf("?")), realSql);
//            }
//        }
//        return null;
//    }
//
//    private String getRealSql(String sql, Statement statement) {
//        if (U.isBlank(sql)) {
//            if (U.isNotBlank(statement)) {
//                sql = statement.toString();
//                if (U.isNotBlank(sql)) {
//                    int i = sql.indexOf(':');
//                    if (i > 0) {
//                        sql = sql.substring(i + 1).trim();
//                    }
//                }
//            }
//        }
//        if (U.isNotBlank(sql)) {
//            // druid -> SQLUtils.formatMySql
//            sql = SqlFormat.format(sql.replaceFirst("^\\s*?\n", ""));
//        }
//        return sql.split("\n").length > 1 ? ("\n" + sql) : sql;
//    }
//
//    @Override
//    public ResultSetInternalMethods postProcess(String sql, Statement statement, ResultSetInternalMethods resultSet,
//                                                Connection connection) throws SQLException {
//        if (LogUtil.SQL_LOG.isDebugEnabled()) {
//            String realSql = getRealSql(sql, statement);
//            if (U.isNotBlank(realSql)) {
//                Thread currentThread = Thread.currentThread();
//                String counterAndTime = TIME_CACHE.getIfPresent(currentThread);
//                if (U.isNotBlank(counterAndTime)) {
//                    try {
//                        String[] split = counterAndTime.split(TIME_SPLIT);
//                        if (split.length == 2) {
//                            long counter = U.toLong(split[0]);
//                            long start = U.toLong(split[1]);
//
//                            StringBuilder sbd = new StringBuilder();
//                            if (U.greater0(counter)) {
//                                sbd.append("counter: ").append(counter);
//                            }
//                            if (U.greater0(start)) {
//                                sbd.append(", use-time: ").append(DateUtil.toHuman(System.currentTimeMillis() - start));
//                            }
//                            if (resultSet != null && resultSet.reallyResult() && resultSet.last()) {
//                                int size = resultSet.getRow();
//                                resultSet.beforeFirst();
//
//                                sbd.append(", return-size: ").append(size);
//                            }
//                            LogUtil.SQL_LOG.debug(sbd.toString());
//                        }
//                    } finally {
//                        TIME_CACHE.invalidate(currentThread);
//                    }
//                }
//            }
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
