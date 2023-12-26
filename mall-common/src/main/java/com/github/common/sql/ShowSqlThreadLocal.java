package com.github.common.sql;

import com.github.common.util.U;

public class ShowSqlThreadLocal {

    private static final ThreadLocal<Boolean> PRINT_SQL_LOCAL = new ThreadLocal<>();

    public static void setPrint() {
        PRINT_SQL_LOCAL.set(true);
    }
    public static boolean hasPrint() {
        return U.toBool(PRINT_SQL_LOCAL.get());
    }
    public static void clear() {
        PRINT_SQL_LOCAL.remove();
    }
}
