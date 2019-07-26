package com.github.user.service;

import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.user.model.UserTest;

/**
 * 用户相关的接口
 */
public interface UserTestService {

    /** 示例接口 */
    PageInfo<UserTest> example(UserTest param, Page page);
}
