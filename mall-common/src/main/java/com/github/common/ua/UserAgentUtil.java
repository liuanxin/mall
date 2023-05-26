package com.github.common.ua;

import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class UserAgentUtil {

    /** https://cdnjs.com/libraries/UAParser.js */
    private static final String UA_PARSE_JS_file = "ua-parser.min.js";

    private static final String PARSE_METHOD = "parseInfo";

    /** https://stackoverflow.com/questions/71481562/use-javascript-scripting-engine-in-java-17 */
    private static final Invocable SCRIPT_ENGINE;
    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngine engine = null;
        try {
            engine = new ScriptEngineManager().getEngineByName("js");
            URL url = UserAgentUtil.class.getClassLoader().getResource(UA_PARSE_JS_file);
            if (U.isNotNull(url)) {
                String uaParserJs = Files.readString(new File(url.getPath()).toPath());
                String parser = uaParserJs.replace("(?=lg)?", "(?=lg)");
                String js = ";function " + PARSE_METHOD + "(ua){return JSON.stringify(new UAParser(ua).getResult());}";
                engine.eval(parser + js);
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("file read exception", e);
            }
        } catch (ScriptException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("js file eval exception", e);
            }
        }
        SCRIPT_ENGINE = U.isNull (engine) ? null : (Invocable) engine;
    }

    public static UserAgentInfo parse(String ua) {
        if (U.isNull(SCRIPT_ENGINE) || U.isBlank(ua)) {
            return null;
        }

        try {
            Object obj = SCRIPT_ENGINE.invokeFunction(PARSE_METHOD, ua);
            return JsonUtil.toObjectNil(U.toStr(obj), UserAgentInfo.class);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("parse ua({}) exception", ua, e);
            }
            return null;
        }
    }
}
