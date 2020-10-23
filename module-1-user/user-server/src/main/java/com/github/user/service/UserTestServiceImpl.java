package com.github.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.U;
import com.github.user.model.UserTest;
import com.github.user.repository.UserTestMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户模块的接口实现类
 */
@Service
@AllArgsConstructor
public class UserTestServiceImpl implements UserTestService {

    private final UserTestMapper userTestMapper;

    @Override
    public PageInfo<UserTest> example(UserTest param, Page page) {
        LambdaQueryWrapper<UserTest> query = Wrappers.lambdaQuery(UserTest.class)
                .select(UserTest::getId, UserTest::getNickName, UserTest::getAvatarUrl);
        if (U.isNotBlank(param)) {
            query.and(i -> i.eq(U.isNotBlank(param.getGender()), UserTest::getGender, param.getGender().getCode())
                    .eq(U.isNotBlank(param.getLevel()), UserTest::getLevel, param.getLevel().getCode())
                    .like(U.isNotBlank(param.getNickName()), UserTest::getNickName, U.like(param.getNickName()))
            );
        }
        // 分页查询并返回
        return Pages.returnPage(userTestMapper.selectPage(Pages.param(page), query));
    }
}
