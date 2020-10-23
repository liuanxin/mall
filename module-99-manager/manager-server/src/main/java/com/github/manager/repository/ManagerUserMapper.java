package com.github.manager.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.manager.model.ManagerUser;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerUserMapper extends BaseMapper<ManagerUser> {
}
