package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.date.DateUtil;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.mq.constant.MqConst;
import com.github.mq.model.MqSend;
import com.github.mq.repository.MqSendDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
        // ( 状态是初始 且 创建时间是在 2 分钟之前 ) 或 ( 状态是失败 且 重试次数小于指定数量 )
        LambdaQueryWrapper<MqSend> query = Wrappers.lambdaQuery(MqSend.class)
                .select(MqSend::getId)
                .or(q -> q.eq(MqSend::getStatus, MqConst.INIT).lt(MqSend::getCreateTime, DateUtil.addMinute(DateUtil.now(), -2)))
                .or(q -> q.eq(MqSend::getStatus, MqConst.FAIL).lt(MqSend::getRetryCount, maxRetryCount))
                .orderByAsc(MqSend::getUpdateTime);
        // 表中有 text 字段, 因此先只查出 id, 再用 id 查具体的数据
        List<MqSend> sendList = Pages.returnList(mqSendDao.selectPage(Pages.paramOnlyLimit(limit), query));
        return A.isEmpty(sendList) ? Collections.emptyList() : mqSendDao.selectBatchIds(A.collect(sendList, MqSend::getId));
    }
}
