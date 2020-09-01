package com.github.manager.repository;

import com.github.liuanxin.page.model.PageBounds;
import com.github.manager.model.ManagerRoleMenu;
import com.github.manager.model.ManagerRoleMenuExample;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRoleMenuMapper {
    long countByExample(ManagerRoleMenuExample example);

    int deleteByExample(ManagerRoleMenuExample example);

    int insertSelective(ManagerRoleMenu record);

    List<ManagerRoleMenu> selectByExample(ManagerRoleMenuExample example, PageBounds page);

    List<ManagerRoleMenu> selectByExample(ManagerRoleMenuExample example);

    int updateByExampleSelective(@Param("record") ManagerRoleMenu record, @Param("example") ManagerRoleMenuExample example);

    int batchInsert(@Param("list") List<ManagerRoleMenu> list);
}
