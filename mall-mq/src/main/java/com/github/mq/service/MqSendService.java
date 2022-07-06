package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.page.Pages;
import com.github.mq.constant.MqConst;
import com.github.mq.model.MqSend;
import com.github.mq.repository.MqSendDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public MqSend queryByMsgId(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<MqSend> query = Wrappers.lambdaQuery(MqSend.class)
                .select(MqSend::getId, MqSend::getRetryCount)
                .eq(MqSend::getMsgId, msgId);
        return Pages.returnOne(mqSendDao.selectPage(Pages.paramOnlyLimit(1), query));
    }

    public List<MqSend> queryRetryMsg(int maxRetryCount, int limit) {
        LambdaQueryWrapper<MqSend> query = Wrappers.lambdaQuery(MqSend.class)
                .eq(MqSend::getStatus, MqConst.FAIL)
                .lt(MqSend::getRetryCount, maxRetryCount)
                .orderByAsc(MqSend::getUpdateTime);
        return Pages.returnList(mqSendDao.selectPage(Pages.paramOnlyLimit(limit), query));
    }
}
