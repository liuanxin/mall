package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Pages;
import com.github.mq.model.MqReceiveEntity;
import com.github.mq.repository.MqReceiveDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MqReceiveService {

    private final MqReceiveDao mqReceiveDao;

    @Transactional
    public void addOrUpdate(MqReceiveEntity record) {
        if (record != null) {
            mqReceiveDao.insertOrUpdate(record);
        }
    }

    public MqReceiveEntity queryByMsg(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<MqReceiveEntity> query = Wrappers.lambdaQuery(MqReceiveEntity.class)
                .select(MqReceiveEntity::getId, MqReceiveEntity::getRetryCount)
                .eq(MqReceiveEntity::getMsgId, msgId);
        return Pages.returnOne(mqReceiveDao.selectPage(Pages.paramOnlyLimit(1), query));
    }
}
