package com.github.common.sql;

import com.github.common.util.Obj;

public class ShowSqlThreadLocal {

    private static final ThreadLocal<Boolean> PRINT_SQL_LOCAL = new ThreadLocal<>();

    public static void setPrint() {
        PRINT_SQL_LOCAL.set(true);
    }
    public static boolean hasPrint() {
        return Obj.defaultIfNull(PRINT_SQL_LOCAL.get(), false);
    }
    public static void clean() {
        PRINT_SQL_LOCAL.remove();
    }
}
