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
    public void add(MqReceive data) {
        if (data != null) {
            mqReceiveMapper.insert(data);
        }
    }

    @Transactional
    public void updateById(MqReceive data) {
        if (data != null) {
            mqReceiveMapper.update(data);
        }
    }

    public MqReceive queryByMsg(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        MqReceiveTableDef mrDef = MqReceiveTableDef.MQ_RECEIVE;
        QueryWrapper query = QueryWrapper.create().select(mrDef.ID, mrDef.RETRY_COUNT).and(mrDef.MSG_ID.eq(msgId));
        return Pages.returnOne(mqReceiveMapper.paginate(Pages.paramOnlyLimit(1), query));
    }

    public List<MqReceive> queryRetryMsg(int maxRetryCount, int limit) {
        // select id ... where ( status = .. and create < .. ) or ( status = .. and retry < .. ) order by update
        // ( 状态是初始 且 创建时间是在 2 分钟之前 ) 或 ( 状态是失败 且 重试次数小于指定数量 )
        MqReceiveTableDef mrDef = MqReceiveTableDef.MQ_RECEIVE;
        QueryWrapper query = QueryWrapper.create()
                .select(mrDef.ID)
                .or(mrDef.STATUS.eq(MqConst.INIT).and(mrDef.CREATE_TIME.lt(DateUtil.addMinute(DateUtil.now(), -2))))
                .or(mrDef.STATUS.eq(MqConst.FAIL).and(mrDef.RETRY_COUNT.lt(maxRetryCount)))
                .orderBy(mrDef.UPDATE_TIME.asc());
        // 表中有 text 字段, 因此先只查出 id, 再用 id 查具体的数据
        List<MqReceive> receiveList = Pages.returnList(mqReceiveMapper.paginate(Pages.paramOnlyLimit(limit), query));
        return A.isEmpty(receiveList) ? Collections.emptyList() : mqReceiveMapper.selectListByIds(A.collect(receiveList, MqReceive::getId));
    }
}
