package com.github.product.repository;

import com.github.liuanxin.page.model.PageBounds;
import com.github.product.model.ProductTest;
import com.github.product.model.ProductTestExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductTestMapper {
    int countByExample(ProductTestExample example);

    int deleteByExample(ProductTestExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(ProductTest record);

    List<ProductTest> selectByExample(ProductTestExample example, PageBounds page);

    List<ProductTest> selectByExample(ProductTestExample example);

    ProductTest selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ProductTest record, @Param("example") ProductTestExample example);

    int updateByPrimaryKeySelective(ProductTest record);
}
