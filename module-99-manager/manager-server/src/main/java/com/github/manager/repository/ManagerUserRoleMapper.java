package com.github.manager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.manager.model.ManagerUserRole;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerUserRoleMapper extends BaseMapper<ManagerUserRole> {

    int batchInsert(@Param("list") List<ManagerUserRole> list);
}
