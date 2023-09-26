package com.github.mq.service;

import com.github.common.date.DateUtil;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.mq.constant.MqConst;
import com.github.mq.model.MqReceive;
import com.github.mq.model.table.MqReceiveTableDef;
import com.github.mq.repository.MqReceiveMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class MqReceiveService {

    private final MqReceiveMapper mqReceiveMapper;

    @Transactional
    public void add(MqReceive record) {
        if (record != null) {
            mqReceiveMapper.insert(record);
        }
    }

    @Transactional
    public void updateById(MqReceive record) {
        if (record != null) {
            mqReceiveMapper.update(record);
        }
    }

    public MqReceive queryByMsg(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        QueryWrapper query = QueryWrapper.create()
                .select(MqReceiveTableDef.MQ_RECEIVE.ID, MqReceiveTableDef.MQ_RECEIVE.RETRY_COUNT)
                .and(MqReceiveTableDef.MQ_RECEIVE.MSG_ID.eq(msgId));
        return Pages.returnOne(mqReceiveMapper.paginate(Pages.paramOnlyLimit(1), query));
    }

    public List<MqReceive> queryRetryMsg(int maxRetryCount, int limit) {
        // select id ... where ( status = .. and create < .. ) or ( status = .. and retry < .. ) order by update
        // ( 状态是初始 且 创建时间是在 2 分钟之前 ) 或 ( 状态是失败 且 重试次数小于指定数量 )
        QueryWrapper query = QueryWrapper.create()
                .select(MqReceiveTableDef.MQ_RECEIVE.ID)
                .or(MqReceiveTableDef.MQ_RECEIVE.STATUS.eq(MqConst.INIT).and(MqReceiveTableDef.MQ_RECEIVE.CREATE_TIME.lt(DateUtil.addMinute(DateUtil.now(), -2))))
                .or(MqReceiveTableDef.MQ_RECEIVE.STATUS.eq(MqConst.FAIL).and(MqReceiveTableDef.MQ_RECEIVE.RETRY_COUNT.lt(maxRetryCount)))
                .orderBy(MqReceiveTableDef.MQ_RECEIVE.UPDATE_TIME.asc());
        // 表中有 text 字段, 因此先只查出 id, 再用 id 查具体的数据
        List<MqReceive> receiveList = Pages.returnList(mqReceiveMapper.paginate(Pages.paramOnlyLimit(limit), query));
        return A.isEmpty(receiveList) ? Collections.emptyList() : mqReceiveMapper.selectListByIds(A.collect(receiveList, MqReceive::getId));
    }
}
