package com.github.common.util;

import com.github.common.bean.BeanChange;
import com.github.common.bean.CollectProperty;
import com.github.common.date.Dates;
import org.junit.Test;

import java.util.Date;

public class BeanTest {

    public static class Abc {
        private int id;

        @CollectProperty("类型")
        private int type;

        @CollectProperty(value = "名字", order = 1)
        private String name;

        @CollectProperty(value = "是否删除", group = {CollectProperty.Group.DELETE}, valueMapping = "{\"0\":\"未删除\",\"OTHER\":\"已删除\"}", order = 5)
        private Integer isDeleted;

        @CollectProperty(value = "创建时间", group = {CollectProperty.Group.CREATE}, dateFormat = "yyyy-MM-dd", order = 3)
        private Date createTime;

        @CollectProperty(value = "更新时间", group = {CollectProperty.Group.CREATE, CollectProperty.Group.UPDATE}, order = 4)
        private Date updateTime;

        @CollectProperty(value = "性别", valueMapping = "{\"0\":\"未知\",\"1\":\"男\",\"2\":\"女\"}", order = 2)
        private int gender;

        private String status;

        private String state;

        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }

        public int getType() {
            return type;
        }
        public void setType(int type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public Integer getIsDeleted() {
            return isDeleted;
        }
        public void setIsDeleted(Integer isDeleted) {
            this.isDeleted = isDeleted;
        }

        public Date getCreateTime() {
            return createTime;
        }
        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }
        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        public int getGender() {
            return gender;
        }
        public void setGender(int gender) {
            this.gender = gender;
        }

        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }

        public String getState() {
            return state;
        }
        public void setState(String state) {
            this.state = state;
        }
    }

    @Test
    public void bean() {
        Abc a1 = new Abc();
        a1.setId(123);
        a1.setType(1);
        a1.setName("张三");
        a1.setCreateTime(Dates.parseToDate("2021-09-09"));
        a1.setUpdateTime(Dates.parseToDate("2021-09-09"));
        a1.setGender(1);
        a1.setStatus("abc");
        a1.setState("xyz");

        Abc a2 = new Abc();
        a2.setId(1234);
        a2.setType(2);
        a2.setName("李四");
        a2.setCreateTime(Dates.parseToDate("2021-09-10"));
        a2.setUpdateTime(Dates.parseToDate("2021-09-10"));
        a2.setIsDeleted(0);
        a2.setGender(2);
        a2.setStatus("");
        a2.setState(null);

        System.out.println(BeanChange.diff(a1, null));
        System.out.println(BeanChange.diff(null, a2));
        System.out.println(BeanChange.diff(a1, a2));
        System.out.println(BeanChange.diff(CollectProperty.Group.CREATE, a1, a2));
        System.out.println(BeanChange.diff(CollectProperty.Group.UPDATE, a1, a2));
        System.out.println(BeanChange.diff(CollectProperty.Group.DELETE, a1, a2));
    }
}
