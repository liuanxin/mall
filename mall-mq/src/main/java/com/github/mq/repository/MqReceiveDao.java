package com.github.mq.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.mq.model.MqReceiveEntity;
import org.apache.ibatis.annotations.Mapper;

/** mq 消费记录 --> mq_receive */
@Mapper
public interface MqReceiveDao extends BaseMapper<MqReceiveEntity> {

    int insertOrUpdate(MqReceiveEntity record);
}
