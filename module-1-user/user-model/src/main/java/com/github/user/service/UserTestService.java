package com.github.user.service;

import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExtend;

/**
 * 用户相关的接口
 */
public interface UserTestService {

    /** 示例接口 */
    PageReturn<UserTest> example(UserTest userTest, UserTestExtend userTestExtend, PageParam page);
}
