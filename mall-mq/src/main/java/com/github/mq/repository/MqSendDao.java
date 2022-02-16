package com.github.mq.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.mq.model.MqSendEntity;
import org.apache.ibatis.annotations.Mapper;

/** mq 生产记录 --> mq_send */
@Mapper
public interface MqSendDao extends BaseMapper<MqSendEntity> {
}
