package com.github.common.util;

import com.github.common.xml.XmlUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import org.junit.Test;

public class XmlTest {

    @Test
    public void test() {
        Abc abc = new Abc();
        abc.setId(123L);
        abc.setName("abc");
        System.out.println(XmlUtil.toXml(abc));

        System.out.println("=================");

        String xml = "<abc><id>123</id><name>abc</name></abc>";
        System.out.println(XmlUtil.toObject(xml, Abc.class));
    }


    @Data
    @XmlRootElement(name = "abc")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Abc {

        private Long id;

        private String name;
    }
}
