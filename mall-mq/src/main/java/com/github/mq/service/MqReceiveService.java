package com.github.mq.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.common.date.DateUtil;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.mq.constant.MqConst;
import com.github.mq.model.MqReceive;
import com.github.mq.repository.MqReceiveDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
        // ( 状态是初始 且 创建时间是在 2 分钟之前 ) 或 ( 状态是失败 且 重试次数小于指定数量 )
        LambdaQueryWrapper<MqReceive> query = Wrappers.lambdaQuery(MqReceive.class)
                .select(MqReceive::getId)
                .or(q -> q.eq(MqReceive::getStatus, MqConst.INIT).lt(MqReceive::getCreateTime, DateUtil.addMinute(DateUtil.now(), -2)))
                .or(q -> q.eq(MqReceive::getStatus, MqConst.FAIL).lt(MqReceive::getRetryCount, maxRetryCount))
                .orderByAsc(MqReceive::getUpdateTime);
        // 表中有 text 字段, 因此先只查出 id, 再用 id 查具体的数据
        List<MqReceive> receiveList = Pages.returnList(mqReceiveDao.selectPage(Pages.paramOnlyLimit(limit), query));
        return A.isEmpty(receiveList) ? Collections.emptyList() : mqReceiveDao.selectBatchIds(A.collect(receiveList, MqReceive::getId));
    }
}
