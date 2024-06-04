package com.github.common.ua;

import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;

/** 解析 user-agent 值, 使用 javascript 的 ua parser 库 */
public class UserAgentUtil {

    /** https://cdnjs.com/libraries/UAParser.js */
    private static final String UA_PARSE_JS_FILE = "ua-parser.min.js";

    private static final String PARSE_METHOD = "parseInfo";

    /** 需要引入额外的包 https://stackoverflow.com/questions/71481562/use-javascript-scripting-engine-in-java-17 */
    private static final Invocable SCRIPT_ENGINE;
    static {
        String uaParserJs = readFile();
        if (U.isNotNull(uaParserJs)) {
            System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript"); // js nashorn 都可以
            try {
                // java 运行 js 库不支持下面的正则, 需要处理一下才行
                String parser = uaParserJs.replace("(?=lg)?", "(?=lg)");
                String js = ";function " + PARSE_METHOD + "(ua){return JSON.stringify(new UAParser(ua).getResult());}";
                engine.eval(parser + js);
            } catch (ScriptException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("js file eval exception", e);
                }
            }
            SCRIPT_ENGINE = (Invocable) engine;
        } else {
            SCRIPT_ENGINE = null;
        }
    }
    private static String readFile() {
        // https://stackoverflow.com/questions/20389255/reading-a-resource-file-from-within-jar
        try (InputStream in = UserAgentUtil.class.getClassLoader().getResourceAsStream(UA_PARSE_JS_FILE)) {
            if (U.isNotNull(in)) {
                return U.inputStreamToString(in);
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("class load file({}) exception", UA_PARSE_JS_FILE, e);
            }
        }
        try (InputStream in = UserAgentUtil.class.getResourceAsStream(UA_PARSE_JS_FILE)) {
            if (U.isNotNull(in)) {
                return U.inputStreamToString(in);
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("load file({}) exception", UA_PARSE_JS_FILE, e);
            }
        }
        return null;
    }

    public static UserAgent parse(String ua) {
        return JsonUtil.toObjectNil(parseStr(ua), UserAgent.class);
    }

    public static String parseStr(String ua) {
        if (U.isNull(SCRIPT_ENGINE) || U.isBlank(ua)) {
            return U.EMPTY;
        }

        try {
            return U.toStr(SCRIPT_ENGINE.invokeFunction(PARSE_METHOD, ua));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("parse ua({}) exception", ua, e);
            }
            return U.EMPTY;
        }
    }
}
