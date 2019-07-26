package com.github.product.service;

import com.github.product.model.ProductTest;

import java.util.List;

public interface ProductTestService {

    /** 获取商品信息 */
    List<ProductTest> example(List<Long> userIdList, ProductTest param);
}
