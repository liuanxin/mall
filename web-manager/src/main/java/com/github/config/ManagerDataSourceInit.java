package com.github.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.common.constant.CommonConst;
import com.github.manager.constant.ManagerConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = { CommonConst.SCAN, UserConst.SCAN, ProductConst.SCAN, OrderConst.SCAN, ManagerConst.SCAN })
public class ManagerDataSourceInit {

    // 3.4.0 开始 PaginationInterceptor 不再建议使用. 见: https://mybatis.plus/guide/interceptor.html

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
