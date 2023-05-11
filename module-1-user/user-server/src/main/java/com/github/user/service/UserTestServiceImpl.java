package com.github.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.common.page.Pages;
import com.github.common.util.U;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExtend;
import com.github.user.repository.UserTestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户模块的接口实现类
 */
@Service
@RequiredArgsConstructor
public class UserTestServiceImpl implements UserTestService {

    private final UserTestMapper userTestMapper;

    @Override
    public PageReturn<UserTest> example(UserTest userTest, UserTestExtend userTestExtend, PageParam page) {
        LambdaQueryWrapper<UserTest> query = Wrappers.lambdaQuery(UserTest.class)
                .select(UserTest::getId, UserTest::getUserName, UserTest::getLevel);
        if (U.isNotNull(userTest)) {
            query.eq(U.isNotBlank(userTest.getUserName()), UserTest::getUserName, userTest.getUserName());
            query.eq(U.isNotBlank(userTest.getPassword()), UserTest::getPassword, userTest.getPassword());
            if (U.isNotNull(userTest.getLevel())) {
                query.eq(UserTest::getLevel, userTest.getLevel().getCode());
            }
        }
        return Pages.returnPage(userTestMapper.selectPage(Pages.param(page), query));
    }
}
