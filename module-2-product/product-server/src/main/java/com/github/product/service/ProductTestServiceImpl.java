package com.github.product.service;

import com.github.common.util.A;
import com.github.common.util.U;
import com.github.product.model.ProductTest;
import com.github.product.model.table.ProductTestTableDef;
import com.github.product.repository.ProductTestMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductTestServiceImpl implements ProductTestService {

    private final ProductTestMapper productTestMapper;

    @Override
    public List<ProductTest> example(List<Long> userIdList, ProductTest param) {
        ProductTestTableDef ptDef = ProductTestTableDef.PRODUCT_TEST;
        QueryWrapper query = QueryWrapper.create();
        if (A.isNotEmpty(userIdList)) {
            query.and(ptDef.USER_ID.in(userIdList));
        }
        if (U.isNotNull(param)) {
            if (U.isNotNull(param.getType())) {
                query.and(ptDef.TYPE.eq(param.getType()));
            }
            if (U.isNotBlank(param.getName())) {
                query.and(ptDef.NAME.likeRight(param.getName()));
            }
        }
        return productTestMapper.selectListByQuery(query);
    }
}
