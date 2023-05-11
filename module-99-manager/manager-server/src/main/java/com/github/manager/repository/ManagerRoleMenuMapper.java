package com.github.manager.repository;

import com.github.manager.model.ManagerRoleMenu;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRoleMenuMapper extends BaseMapper<ManagerRoleMenu> {

    int batchInsert(@Param("list") List<ManagerRoleMenu> list);
}
