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
//import java.util.regex.Pattern;
//
///**
// * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
// * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSqlInterceptor
// */
//public class ShowSql5Interceptor implements StatementInterceptor {
//
//    private static final String TIME_SPLIT = "~";
//    private static final AtomicLong ID = new AtomicLong(0L);
//    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
//    private static final Cache<Thread, String> TIME_CACHE = CacheBuilder.newBuilder()
//            .expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(50000L).build();
//    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{1,}");
//
//    @Override
//    public void init(Connection connection, Properties properties) throws SQLException {}
//
//    @Override
//    public ResultSetInternalMethods preProcess(String sql, Statement statement, Connection connection) throws SQLException {
//        if (ShowSqlThreadLocal.hasPrint() && LogUtil.SQL_LOG.isDebugEnabled()) {
//            String realSql = getRealSql(sql, statement);
//            if (U.isNotBlank(realSql)) {
//                long id = ID.addAndGet(1);
//                TIME_CACHE.put(Thread.currentThread(), id + TIME_SPLIT + System.currentTimeMillis());
//                String url = connection.getMetaData().getURL();
//                // connection.getHost() + ":" + connection.getSocksProxyPort();
//                LogUtil.SQL_LOG.debug("id: {}, data-source: {}, sql: {}", id,
//                        url.substring(url.indexOf("//") + 2, url.indexOf("?")), realSql);
//            }
//        }
//        return null;
//    }
//
//    private String getRealSql(String sql, Statement statement) {
//        if (U.isEmpty(sql)) {
//            if (U.isNotNull(statement)) {
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
//            // sql = SQLUtils.formatMySql(sql.replaceFirst("^\\s*?\n", ""));
//            // sql = SqlFormat.format(sql.replaceFirst("^\\s*?\n", ""));
//            sql = BLANK_REGEX.matcher(sql.replaceFirst("^\\s*?\n", "")).replaceAll(" ");
//        }
//        return sql;
//    }
//
//    @Override
//    public ResultSetInternalMethods postProcess(String sql, Statement statement, ResultSetInternalMethods resultSet,
//                                                Connection connection) throws SQLException {
//        if (ShowSqlThreadLocal.hasPrint() && LogUtil.SQL_LOG.isDebugEnabled()) {
//            String realSql = getRealSql(sql, statement);
//            if (U.isNotBlank(realSql)) {
//                Thread currentThread = Thread.currentThread();
//                String idAndTime = TIME_CACHE.getIfPresent(currentThread);
//                if (U.isNotBlank(idAndTime)) {
//                    try {
//                        String[] split = idAndTime.split(TIME_SPLIT);
//                        if (split.length == 2) {
//                            long id = U.toLong(split[0]);
//                            long start = U.toLong(split[1]);
//
//                            StringBuilder sbd = new StringBuilder();
//                            if (U.greater0(id)) {
//                                sbd.append("id: ").append(id);
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
