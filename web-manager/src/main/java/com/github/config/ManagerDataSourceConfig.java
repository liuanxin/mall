package com.github.config;

import com.github.common.constant.CommonConst;
import com.github.manager.constant.ManagerConst;
import com.github.order.constant.OrderConst;
import com.github.product.constant.ProductConst;
import com.github.user.constant.UserConst;
import com.mybatisflex.core.FlexGlobalConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = { CommonConst.SCAN, UserConst.SCAN, ProductConst.SCAN, OrderConst.SCAN, ManagerConst.SCAN })
public class ManagerDataSourceConfig {

    static {
        FlexGlobalConfig globalConfig = FlexGlobalConfig.getDefaultConfig();
        globalConfig.setPrintBanner(false);
        globalConfig.setNormalValueOfLogicDelete("0");
        globalConfig.setDeletedValueOfLogicDelete("id");
    }
}
