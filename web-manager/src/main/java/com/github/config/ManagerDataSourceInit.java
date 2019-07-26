package com.github.config;

import com.github.common.constant.CommonConst;
import com.github.liuanxin.page.PageInterceptor;
import com.github.manager.constant.ManagerConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = { CommonConst.SCAN, UserConst.SCAN, ProductConst.SCAN, OrderConst.SCAN, ManagerConst.SCAN })
public class ManagerDataSourceInit {

    private final DataSource dataSource;

    public ManagerDataSourceInit(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        // 装载 xml 实现
        sessionFactory.setMapperLocations(WebManagerConfigData.RESOURCE_ARRAY);
        // 装载 typeHandler 实现
        sessionFactory.setTypeHandlers(WebManagerConfigData.HANDLER_ARRAY);
        // mybatis 的分页插件
        sessionFactory.setPlugins(new Interceptor[] { new PageInterceptor("mysql") });
        return sessionFactory.getObject();
    }

    /** 要构建 or 语句, 参考: http://www.mybatis.org/generator/generatedobjects/exampleClassUsage.html */
    @Bean(name = "sqlSessionTemplate", destroyMethod = "clearCache")
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }
}
