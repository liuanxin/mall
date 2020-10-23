package com.github.manager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.manager.model.ManagerRoleMenu;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRoleMenuMapper extends BaseMapper<ManagerRoleMenu> {

    int batchInsert(@Param("list") List<ManagerRoleMenu> list);
}
