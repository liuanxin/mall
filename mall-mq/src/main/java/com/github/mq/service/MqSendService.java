package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Pages;
import com.github.mq.model.MqSend;
import com.github.mq.repository.MqSendDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MqSendService {

    private final MqSendDao mqSendDao;

    @Transactional
    public void add(MqSend record) {
        if (record != null) {
            mqSendDao.insert(record);
        }
    }

    @Transactional
    public void updateById(MqSend record) {
        if (record != null) {
            mqSendDao.updateById(record);
        }
    }

    @Transactional
    public void updateByMsgId(String msgId, MqSend record) {
        mqSendDao.update(record, Wrappers.lambdaQuery(MqSend.class).eq(MqSend::getMsgId, msgId));
    }

    public MqSend queryByMsgId(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<MqSend> query = Wrappers.lambdaQuery(MqSend.class)
                .select(MqSend::getId, MqSend::getRetryCount)
                .eq(MqSend::getMsgId, msgId);
        return Pages.returnOne(mqSendDao.selectPage(Pages.paramOnlyLimit(1), query));
    }
}