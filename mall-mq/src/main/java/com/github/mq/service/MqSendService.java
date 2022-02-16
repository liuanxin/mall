package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Pages;
import com.github.mq.model.MqSendEntity;
import com.github.mq.repository.MqSendDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MqSendService {

    private final MqSendDao mqSendDao;

    @Transactional
    public void add(MqSendEntity record) {
        if (record != null) {
            mqSendDao.insert(record);
        }
    }

    @Transactional
    public void updateById(MqSendEntity record) {
        if (record != null) {
            mqSendDao.updateById(record);
        }
    }

    @Transactional
    public int updateByMsgId(String msgId, MqSendEntity record) {
        return mqSendDao.update(record, Wrappers.lambdaQuery(MqSendEntity.class).eq(MqSendEntity::getMsgId, msgId));
    }

    public MqSendEntity queryByMsgId(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<MqSendEntity> query = Wrappers.lambdaQuery(MqSendEntity.class)
                .select(MqSendEntity::getId, MqSendEntity::getRetryCount)
                .eq(MqSendEntity::getMsgId, msgId);
        return Pages.returnOne(mqSendDao.selectPage(Pages.paramOnlyLimit(1), query));
    }
}
