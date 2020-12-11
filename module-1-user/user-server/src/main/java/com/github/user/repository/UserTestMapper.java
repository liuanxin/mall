package com.github.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExtend;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTestMapper extends BaseMapper<UserTest> {

    Page<UserTest> selectUserTestJoin(@Param("record") UserTestExtend userTestExtend, Page<UserTest> page);
}
