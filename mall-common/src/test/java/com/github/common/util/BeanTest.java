package com.github.common.util;

import com.github.common.bean.BeanChange;
import com.github.common.bean.CollectGroup;
import com.github.common.bean.CollectProperty;
import com.github.common.date.DateUtil;
import lombok.Data;
import org.junit.Test;

import java.util.Date;

public class BeanTest {

    @Data
    public static class Abc {
        private int id;
        @CollectProperty(value = "名字", order = 1)
        private String name;
        @CollectProperty(value = "是否删除", collectGroup = {CollectGroup.DELETE}, valueMapping = "{\"0\":\"未删除\",\"OTHER\":\"已删除\"}", order = 5)
        private Integer isDeleted;
        @CollectProperty(value = "创建时间", collectGroup = {CollectGroup.CREATE}, dateFormat = "yyyy-MM-dd", order = 3)
        private Date createTime;
        @CollectProperty(value = "更新时间", collectGroup = {CollectGroup.CREATE, CollectGroup.UPDATE}, order = 4)
        private Date updateTime;
        @CollectProperty(value = "性别", valueMapping = "{\"0\":\"未知\",\"1\":\"男\",\"2\":\"女\"}", order = 2)
        private int type;
    }

    @Test
    public void bean() {
        Abc a1 = new Abc();
        a1.setId(123);
        a1.setName("张三");
        a1.setCreateTime(DateUtil.parse("2021-09-09"));
        a1.setUpdateTime(DateUtil.parse("2021-09-09"));
        a1.setType(1);

        Abc a2 = new Abc();
        a2.setId(123);
        a2.setName("李四");
        a2.setCreateTime(DateUtil.parse("2021-09-10"));
        a2.setUpdateTime(DateUtil.parse("2021-09-10"));
        a2.setIsDeleted(0);
        a2.setType(2);

        System.out.println(BeanChange.diff(a1, null));
        System.out.println(BeanChange.diff(null, a2));
        System.out.println(BeanChange.diff(a1, a2));
        System.out.println(BeanChange.diff(CollectGroup.CREATE, a1, a2));
        System.out.println(BeanChange.diff(CollectGroup.UPDATE, a1, a2));
        System.out.println(BeanChange.diff(CollectGroup.DELETE, a1, a2));
    }
}
