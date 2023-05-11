package com.github.manager.repository;

import com.github.manager.model.ManagerRolePermission;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerRolePermissionMapper extends BaseMapper<ManagerRolePermission> {

    int batchInsert(@Param("list") List<ManagerRolePermission> list);
}
