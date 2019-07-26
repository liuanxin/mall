package com.github.user.repository;

import com.github.liuanxin.page.model.PageBounds;
import com.github.user.model.UserTest;
import com.github.user.model.UserTestExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserTestMapper {
    int countByExample(UserTestExample example);

    int deleteByExample(UserTestExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(UserTest record);

    List<UserTest> selectByExample(UserTestExample example, PageBounds page);

    List<UserTest> selectByExample(UserTestExample example);

    UserTest selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UserTest record, @Param("example") UserTestExample example);

    int updateByPrimaryKeySelective(UserTest record);
}
