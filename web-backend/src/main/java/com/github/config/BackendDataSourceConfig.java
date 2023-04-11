package com.github.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.common.constant.CommonConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = { CommonConst.SCAN, UserConst.SCAN, ProductConst.SCAN, OrderConst.SCAN })
public class BackendDataSourceConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
