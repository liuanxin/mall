//package com.github.global.config;
//
//import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
//import com.github.common.util.U;
//import org.apache.ibatis.reflection.MetaObject;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Date;
//
//@Configuration
//@ConditionalOnClass(MetaObjectHandler.class)
//public class MybatisFieldMetaHandler implements MetaObjectHandler {
//
//    private static final String CREATE_TIME = "createTime";
//    private static final String UPDATE_TIME = "updateTime";
//
//    @Override
//    public void insertFill(MetaObject metaObject) {
//        boolean hasCreateTime = U.isNull(getFieldValByName(CREATE_TIME, metaObject));
//        boolean hasUpdateTime = U.isNull(getFieldValByName(UPDATE_TIME, metaObject));
//        if (hasCreateTime || hasUpdateTime) {
//            Date date = new Date();
//
//            if (hasCreateTime) {
//                setFieldValByName(CREATE_TIME, date, metaObject);
//            }
//            if (hasUpdateTime) {
//                setFieldValByName(UPDATE_TIME, date, metaObject);
//            }
//        }
//    }
//
//    @Override
//    public void updateFill(MetaObject metaObject) {
//        if (U.isNull(getFieldValByName(UPDATE_TIME, metaObject))) {
//            setFieldValByName(UPDATE_TIME, new Date(), metaObject);
//        }
//    }
//}
