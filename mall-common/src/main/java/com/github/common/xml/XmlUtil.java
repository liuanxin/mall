package com.github.common.xml;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

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
            // 在属性上标 @XmlJavaTypeAdapter(Cdata.Adapter.class) 在值的前后包值
            // 利用动态代理, 避免想输出成 <![CDATA[ abc ]]> 时却显示成了 &lt;![CDATA[ abc ]]&gt; 的问题
            XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            Class<? extends XMLStreamWriter> writerClass = streamWriter.getClass();
            XMLStreamWriter stream = (XMLStreamWriter) Proxy.newProxyInstance(
                    writerClass.getClassLoader(), writerClass.getInterfaces(), new Cdata.Handler(streamWriter)
            );
            Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 不输出 <?xml version="1.0" encoding="UTF-8" standalone="yes"?> 头
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
