package com.github.common.http;

public final class HttpConst {

    /** 建立连接的超时时间, 单位: 毫秒 */
    public static final int CONNECT_TIME_OUT = 5000;
    /** 数据交互的时间, 单位: 毫秒 */
    public static final int READ_TIME_OUT = 60000;

    public static String getUserAgent(String client) {
        return String.format("Mozilla/5.0 (%s; Win64; x64)" +
                " AppleWebKit/537.36 (KHTML, like Gecko)" +
                " Chrome/100.0.4896.127" +
                " Safari/537.36", client);
    }
}
