package com.github.mq.repository;

import com.github.mq.model.MqSend;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** mq 生产记录 --> mq_send */
@Mapper
public interface MqSendMapper extends BaseMapper<MqSend> {
}
