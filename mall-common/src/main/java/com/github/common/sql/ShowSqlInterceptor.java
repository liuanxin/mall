package com.github.common.sql;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.common.date.DateUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.Session;
import com.mysql.cj.conf.HostInfo;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * mysql 5 的连接参数是: &statementInterceptors=com.github.common.sql.ShowSql5Interceptor
 * mysql 8 的连接参数是: &queryInterceptors=com.github.common.sql.ShowSqlInterceptor
 */
public class ShowSqlInterceptor implements QueryInterceptor {

    private static final String TIME_SPLIT = "~";
    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{1,}");
    private static final AtomicLong ID = new AtomicLong(0L);
    /** 每条 sql 执行前记录时间戳, 如果使用 ThreadLocal 会有 pre 了但运行时异常不去 post 的情况 */
    private static final Cache<Thread, String> SQL_TIME_CACHE =
            Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(30, TimeUnit.MINUTES).build();

    @Override
    public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
        return this;
    }

    @Override
    public <T extends Resultset> T preProcess(Supplier<String> sql, Query query) {
        if (ShowSqlThreadLocal.hasPrint() && LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotBlank(realSql)) {
                long id = ID.addAndGet(1);
                SQL_TIME_CACHE.put(Thread.currentThread(), id + TIME_SPLIT + System.currentTimeMillis());
                String dataSource = "";
                if (U.isNotNull(query)) {
                    Session session = query.getSession();
                    if (U.isNotNull(session)) {
                        HostInfo hostInfo = session.getHostInfo();
                        if (U.isNotNull(hostInfo)) {
                            dataSource = ", db: " + hostInfo.getHost() + ":" + hostInfo.getPort() + "/" + hostInfo.getDatabase();
                        }
                    }
                }
                LogUtil.SQL_LOG.debug("id: {}{}, sql: {}", id, dataSource, realSql);
            }
        }
        return null;
    }

    private String getRealSql(Supplier<String> sql) {
        if (U.isNull(sql)) {
            return null;
        }

        // String realSql = SQLUtils.formatMySql(sql.get().replaceFirst("^\\s*?\n", ""));
        // String realSql = SqlFormat.format(sql.get().replaceFirst("^\\s*?\n", ""));
        return BLANK_REGEX.matcher(sql.get().replaceFirst("^\\s*?\n", "")).replaceAll(" ");
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query query, T rs, ServerSession serverSession) {
        if (ShowSqlThreadLocal.hasPrint() && LogUtil.SQL_LOG.isDebugEnabled()) {
            String realSql = getRealSql(sql);
            if (U.isNotBlank(realSql)) {
                Thread thread = Thread.currentThread();
                String idAndTime = SQL_TIME_CACHE.getIfPresent(thread);
                if (U.isNotNull(idAndTime)) {
                    try {
                        String[] split = idAndTime.split(TIME_SPLIT);
                        if (split.length == 2) {
                            long id = U.toLong(split[0]);
                            long start = U.toLong(split[1]);

                            StringBuilder sbd = new StringBuilder();
                            if (U.greater0(id)) {
                                sbd.append("id: ").append(id);
                            }
                            if (U.greater0(start)) {
                                sbd.append(", use-time: ").append(DateUtil.toHuman(System.currentTimeMillis() - start));
                            }
                            if (U.isNotNull(rs) && rs.hasRows()) {
                                sbd.append(", return-size: ").append(rs.getRows().size());
                            }
                            LogUtil.SQL_LOG.debug(sbd.toString());
                        }
                    } finally {
                        SQL_TIME_CACHE.invalidate(thread);
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
        SQL_TIME_CACHE.cleanUp();
    }
}
