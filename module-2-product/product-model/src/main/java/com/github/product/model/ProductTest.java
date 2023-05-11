package com.github.product.model;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.util.Date;

/** 商品示例 --> t_product_test */
@Data
@Table("t_product_test")
public class ProductTest {

    private Long id;

    /** 所属用户 --> user_id */
    private Long userId;

    /** 商品名(60 个字以内) --> name */
    private String name;

    /** 商品类型 --> type */
    private Integer type;

    /** 创建时间 --> create_time */
    private Date createTime;

    /** 最近更新时间 --> update_time */
    private Date updateTime;

    /** 1 表示已删除 --> is_delete */
    @Column(isLogicDelete = true)
    private Boolean isDelete;
}
