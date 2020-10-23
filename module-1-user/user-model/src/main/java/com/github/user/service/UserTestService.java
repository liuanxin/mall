package com.github.user.service;

import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.user.model.UserTest;

/**
 * 用户相关的接口
 */
public interface UserTestService {

    /** 示例接口 */
    PageReturn<UserTest> example(UserTest param, PageParam page);
}
