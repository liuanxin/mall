package com.github.common.xml;

import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

/**
 * <pre>
 * 当某个 String 类型的值是 a>b&lt;c 时, 默认会显示成 a&#064;gt;b&#064;lt;c, 大于小于转义了.
 * 如果想要输出成 &lt;![CDATA[ a>b&lt;c ]]> 如下操作即可
 *
 * 1. 在类上标 @XmlAccessorType(XmlAccessType.FIELD)
 * 2. 在 String 类型上标 @XmlJavaTypeAdapter(Cdata.Adapter.class)
 * </pre>
 */
public class XmlUtil {

    public static <T> String toXml(T obj) {
        try {
            return convertXml(obj);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Object(%s) to xml exception", obj), e);
        }
    }
    private static <T> String convertXml(T obj) throws Exception {
        if (obj == null) {
            return null;
        }
        try (StringWriter writer = new StringWriter()) {
            XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            Class<? extends XMLStreamWriter> writerClass = streamWriter.getClass();
            XMLStreamWriter stream = (XMLStreamWriter) Proxy.newProxyInstance(
                    writerClass.getClassLoader(), writerClass.getInterfaces(), new Cdata.Handler(streamWriter)
            );
            Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 不输出 <?xml version="1.0" encoding="UTF-8" standalone="yes"?> 头(false 将会输出)
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(obj, stream);
            return writer.toString();
        }
    }
    public static <T> String toXmlNil(T obj) {
        try {
            return convertXml(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Object({}) to xml exception", obj, e);
            }
            return null;
        }
    }

    public static <T> T toObject(String xml, Class<T> clazz) {
        try {
            return convertObject(xml, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("xml(%s) to Object(%s) exception", xml, clazz.getName()), e);
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> T convertObject(String xml, Class<T> clazz) throws Exception {
        return U.isBlank(xml) ? null : (T) JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(new StringReader(xml));
    }
    public static <T> T toObjectNil(String xml, Class<T> clazz) {
        try {
            return convertObject(xml, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("xml({}) to Object({}) exception", xml, clazz.getName(), e);
            }
            return null;
        }
    }
}
