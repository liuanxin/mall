package com.github.user.service;

import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.common.page.Pages;
import com.github.common.util.U;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExtend;
import com.github.user.model.table.UserTestTableDef;
import com.github.user.repository.UserTestMapper;
import com.mybatisflex.core.query.QueryWrapper;
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
        QueryWrapper query = QueryWrapper.create()
                .select(UserTestTableDef.USER_TEST.ID, UserTestTableDef.USER_TEST.USER_NAME, UserTestTableDef.USER_TEST.LEVEL);
        if (U.isNotNull(userTest)) {
            if (U.isNotBlank(userTest.getUserName())) {
                query.and(UserTestTableDef.USER_TEST.USER_NAME.eq(userTest.getUserName()));
            }
            if (U.isNotBlank(userTest.getPassword())) {
                query.and(UserTestTableDef.USER_TEST.PASSWORD.eq(userTest.getPassword()));
            }
            if (U.isNotNull(userTest.getLevel())) {
                query.and(UserTestTableDef.USER_TEST.LEVEL.eq(userTest.getLevel()));
            }
        }
        return Pages.returnPage(userTestMapper.paginate(Pages.param(page), query));
    }
}
