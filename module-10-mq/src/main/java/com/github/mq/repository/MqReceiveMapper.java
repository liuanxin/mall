package com.github.mq.repository;

import com.github.mq.model.MqReceive;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** mq 消费记录 --> mq_receive */
@Mapper
public interface MqReceiveMapper extends BaseMapper<MqReceive> {

    int insertOrUpdate(MqReceive record);
}
