package com.github.user.service;

import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.U;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExample;
import com.github.user.repository.UserTestMapper;
import org.springframework.stereotype.Service;

/**
 * 用户模块的接口实现类
 */
@Service
public class UserTestServiceImpl implements UserTestService {

    private final UserTestMapper userTestMapper;

    public UserTestServiceImpl(UserTestMapper userTestMapper) {
        this.userTestMapper = userTestMapper;
    }

    @Override
    public PageInfo<UserTest> example(UserTest param, Page page) {
        UserTestExample example = new UserTestExample();
        if (U.isNotBlank(param)) {
            // 动态生成 sql: where gender = xx and level = yy and nick_name like '%xyz%'
            UserTestExample.Criteria criteria = example.createCriteria();
            if (U.isNotBlank(param.getGender())) {
                criteria.andGenderEqualTo(param.getGender().getCode());
            }
            if (U.isNotBlank(param.getLevel())) {
                criteria.andLevelEqualTo(param.getLevel().getCode());
            }
            if (U.isNotBlank(param.getNickName())) {
                criteria.andNickNameLike(U.like(param.getNickName()));
            }
        }
        // 分页查询并返回
        return Pages.returnPage(userTestMapper.selectByExample(example, Pages.param(page)));
    }
}
