package com.github.manager.service;

import com.github.common.page.PageInfo;

/**
 * 管理相关的接口
 */
public interface ManagerService {

    /**
     * 示例接口
     *
     * @param xx 参数
     * @param page 当前页
     * @param limit 每页行数
     * @return 分页信息
     */
    PageInfo demo(String xx, Integer page, Integer limit);
}
