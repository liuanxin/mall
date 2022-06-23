package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Pages;
import com.github.mq.model.MqReceive;
import com.github.mq.repository.MqReceiveDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MqReceiveService {

    private final MqReceiveDao mqReceiveDao;

    @Transactional
    public void add(MqReceive record) {
        if (record != null) {
            mqReceiveDao.insert(record);
        }
    }

    @Transactional
    public void updateById(MqReceive record) {
        if (record != null) {
            mqReceiveDao.updateById(record);
        }
    }

    public MqReceive queryByMsg(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<MqReceive> query = Wrappers.lambdaQuery(MqReceive.class)
                .select(MqReceive::getId, MqReceive::getRetryCount)
                .eq(MqReceive::getMsgId, msgId);
        return Pages.returnOne(mqReceiveDao.selectPage(Pages.paramOnlyLimit(1), query));
    }

    public List<MqReceive> queryRetryMsg(int maxRetryCount, int limit) {
        LambdaQueryWrapper<MqReceive> query = Wrappers.lambdaQuery(MqReceive.class)
                .eq(MqReceive::getStatus, 1)
                .lt(MqReceive::getRetryCount, maxRetryCount)
                .orderByAsc(MqReceive::getUpdateTime);
        return Pages.returnList(mqReceiveDao.selectPage(Pages.paramOnlyLimit(limit), query));
    }
}
