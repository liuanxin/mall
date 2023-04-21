package com.github.common.util;

import com.github.common.xml.XmlUtil;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import org.junit.Test;

public class XmlTest {

    @Test
    public void test() throws Exception {
        Abc abc = new Abc();
        abc.setId(123L);
        abc.setName("abc");
        System.out.println(abc.getClass().getAnnotation(XmlRootElement.class));
        System.out.println(XmlUtil.toXml(abc));
        System.out.println("=================");
        String xml = "<abc><id>123</id><name>abc</name></abc>";
        System.out.println(XmlUtil.toObject(xml, Abc.class));

//        ObjectMapper render = new XmlMapper();
//        System.out.println(render.writeValueAsString(abc));
//        System.out.println("=================");
//        System.out.println(render.readValue(xml, Abc.class));
    }


    @Data
    @XmlRootElement(name = "abc")
//    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Abc {

        private Long id;

//        @XmlJavaTypeAdapter(Cdata.Adapter.class)
        private String name;
    }
}
