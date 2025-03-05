package com.github.common.util;

import com.github.common.xml.Cdata;
import com.github.common.xml.XmlUtil;
import org.junit.Test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class XmlTest {

    @Test
    public void test() throws Exception {
        Abc abc = new Abc();
        abc.setId(123L);
        abc.setName("abc>123<中文");
        System.out.println(abc.getClass().getAnnotation(XmlRootElement.class));
        System.out.println("=================");
        System.out.println(XmlUtil.toXml(abc));
        System.out.println("=================");
        String abcXml = "<abc><id>123</id><name>abc&gt;123&lt;中文</name></abc>";
        System.out.println(XmlUtil.toObject(abcXml, Abc.class));

        System.out.println("-----------------");
        Xyz xyz = new Xyz();
        xyz.setId(123L);
        xyz.setName("abc>123<中文");
        System.out.println(xyz.getClass().getAnnotation(XmlRootElement.class));
        System.out.println("=================");
        System.out.println(XmlUtil.toXml(xyz));
        System.out.println("=================");
        String xyzXml = "<xyz><id>123</id><name>abc&gt;123&lt;中文</name></xyz>";
        System.out.println(XmlUtil.toObject(xyzXml, Xyz.class));

//        ObjectMapper render = new XmlMapper();
//        System.out.println(render.writeValueAsString(abc));
//        System.out.println("=================");
//        System.out.println(render.readValue(xml, Abc.class));
    }


    @XmlRootElement(name = "abc")
    public static class Abc {
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    @XmlRootElement(name = "xyz")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Xyz {
        private Long id;

        @XmlJavaTypeAdapter(Cdata.Adapter.class)
        private String name;

        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}
