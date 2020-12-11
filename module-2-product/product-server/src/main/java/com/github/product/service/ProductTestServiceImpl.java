package com.github.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.product.model.ProductTest;
import com.github.product.repository.ProductTestMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductTestServiceImpl implements ProductTestService {

    private final ProductTestMapper productTestMapper;

    @Override
    public List<ProductTest> example(List<Long> userIdList, ProductTest param) {
        LambdaQueryWrapper<ProductTest> query = Wrappers.lambdaQuery(ProductTest.class);

        query.in(A.isNotEmpty(userIdList), ProductTest::getUserId, userIdList);
        if (U.isNotBlank(param)) {
            if (U.isNotBlank(param.getType())) {
                query.eq(ProductTest::getType, param.getType().getCode());
            }
            if (U.isNotBlank(param.getName())) {
                query.likeRight(ProductTest::getName, param.getName());
            }
        }
        return productTestMapper.selectList(query);
    }
}
