package com.github.common.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * <pre>
 * 当某个 String 类型的值是 a>b&lt;c 时, 默认会显示成 a&#064;gt;b&#064;lt;c, 大于小于转义了.
 * 如果想要输出成 &lt;![CDATA[ a>b&lt;c ]]> 如下操作即可
 *
 * 1. 在类上标 @XmlAccessorType(XmlAccessType.FIELD)
 * 2. 在 String 类型上标 @XmlJavaTypeAdapter(Cdata.Adapter.class)
 * </pre>
 */
public class Cdata {

    private static final String START = "<![CDATA[";
    private static final String END = "]]>";

    public static class Adapter extends XmlAdapter<String, String> {
        @Override
        public String marshal(String arg0) {
            return START + arg0 + END;
        }

        @Override
        public String unmarshal(String arg0) {
            return arg0;
        }
    }


    static class Handler implements InvocationHandler {
        private final XMLStreamWriter writer;
        Handler(XMLStreamWriter writer) {
            this.writer = writer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("writeCharacters".equals(method.getName()) && method.getParameterTypes()[0] == char[].class) {
                String text = new String((char[]) args[0]);
                if (text.startsWith(START) && text.endsWith(END)) {
                    writer.writeCData(text.substring(START.length(), text.length() - END.length()));
                    return null;
                }
            }
            return method.invoke(writer, args);
        }
    }
}
