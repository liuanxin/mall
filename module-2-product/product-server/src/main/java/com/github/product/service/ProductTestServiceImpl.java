package com.github.product.service;

import com.github.common.util.A;
import com.github.common.util.U;
import com.github.product.model.ProductTest;
import com.github.product.model.ProductTestExample;
import com.github.product.repository.ProductTestMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductTestServiceImpl implements ProductTestService {

    private final ProductTestMapper productTestMapper;

    public ProductTestServiceImpl(ProductTestMapper productTestMapper) {
        this.productTestMapper = productTestMapper;
    }

    @Override
    public List<ProductTest> example(List<Long> userIdList, ProductTest param) {
        ProductTestExample example = new ProductTestExample();
        // 生成动态 sql: where is_delete = 0 /* 后面动态生成 */ and user_id in (x, xx) and type = xx and name like 'xyz%'
        ProductTestExample.Criteria criteria = example.or().andIsDeleteEqualTo(false);

        if (A.isNotEmpty(userIdList)) {
            criteria.andUserIdIn(userIdList);
        }
        if (U.isNotBlank(param)) {
            if (U.isNotBlank(param.getType())) {
                criteria.andTypeEqualTo(param.getType().getCode());
            }
            if (U.isNotBlank(param.getName())) {
                criteria.andNameLike(U.rightLike(param.getName()));
            }
        }
        // 如果要查询单条数据(不是用 id 查询), 可以用下面的方式
        // productExample single = A.first(productTestMapper.selectByExample(example, new PageBounds(1)));
        return productTestMapper.selectByExample(example);
    }
}
