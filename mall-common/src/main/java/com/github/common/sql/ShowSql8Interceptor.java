package com.github.common.sql;

import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
 * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSql8Interceptor
 */
public class ShowSql8Interceptor implements QueryInterceptor {

    private static final String TIME_SPLIT = "~";
    private static final AtomicLong COUNTER = new AtomicLong(0L);
    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
    private static final Cache<Thread, String> TIME_CACHE = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();

    @Override
    public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
        return this;
    }

    @Override
    public <T extends Resultset> T preProcess(Supplier<String> sql, Query query) {
        if (LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotBlank(realSql)) {
                Thread currentThread = Thread.currentThread();
                long current = System.currentTimeMillis();
                long counter = COUNTER.addAndGet(1);

                TIME_CACHE.put(currentThread, counter + TIME_SPLIT + current);
                LogUtil.SQL_LOG.debug("counter: {}, sql:\n{}", counter, realSql);
            }
        }
        return null;
    }

    private String getRealSql(Supplier<String> sql) {
        // druid -> SQLUtils.formatMySql
        return U.isBlank(sql) ? null : SqlFormat.format(sql.get().replaceFirst("^\\s*?\n", ""));
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query query, T rs, ServerSession serverSession) {
        if (LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotBlank(realSql)) {
                Thread currentThread = Thread.currentThread();
                String counterAndTime = TIME_CACHE.getIfPresent(currentThread);
                if (U.isNotBlank(counterAndTime)) {
                    try {
                        String[] split = counterAndTime.split(TIME_SPLIT);
                        if (split.length == 2) {
                            long counter = U.toLong(split[0]);
                            long start = U.toLong(split[1]);

                            StringBuilder sbd = new StringBuilder();
                            if (U.greater0(counter)) {
                                sbd.append("counter: ").append(counter);
                            }
                            if (U.greater0(start)) {
                                sbd.append(", time: ").append(DateUtil.toHuman(System.currentTimeMillis() - start));
                            }
                            if (U.isNotBlank(rs) && rs.hasRows()) {
                                sbd.append(", size: ").append(rs.getRows().size());
                            }
                            LogUtil.SQL_LOG.debug(sbd.toString());
                        }
                    } finally {
                        TIME_CACHE.invalidate(currentThread);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean executeTopLevelOnly() { return false; }
    @Override
    public void destroy() {
        TIME_CACHE.invalidateAll();
    }
}
