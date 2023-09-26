package com.github.mq.service;

import com.github.common.date.DateUtil;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.mq.constant.MqConst;
import com.github.mq.model.MqSend;
import com.github.mq.model.table.MqSendTableDef;
import com.github.mq.repository.MqSendMapper;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MqSendService {

    private final MqSendMapper mqSendMapper;

    @Transactional
    public void add(MqSend record) {
        if (record != null) {
            mqSendMapper.insert(record);
        }
    }

    @Transactional
    public void updateById(MqSend record) {
        if (record != null) {
            mqSendMapper.update(record);
        }
    }

    public MqSend queryByMsgId(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return null;
        }

        MqSendTableDef msDef = MqSendTableDef.MQ_SEND;
        QueryWrapper query = QueryWrapper.create().select(msDef.ID, msDef.RETRY_COUNT).and(msDef.MSG_ID.eq(msgId));
        return Pages.returnOne(mqSendMapper.paginate(Pages.paramOnlyLimit(1), query));
    }

    public List<MqSend> queryRetryMsg(int maxRetryCount, int limit) {
        // select id ... where ( status = .. and create < .. ) or ( status = .. and retry < .. ) order by update
        // ( 状态是初始 且 创建时间是在 2 分钟之前 ) 或 ( 状态是失败 且 重试次数小于指定数量 )
        MqSendTableDef msDef = MqSendTableDef.MQ_SEND;
        QueryWrapper query = QueryWrapper.create()
                .select(msDef.ID)
                .or(msDef.STATUS.eq(MqConst.INIT).and(msDef.CREATE_TIME.lt(DateUtil.addMinute(DateUtil.now(), -2))))
                .or(msDef.STATUS.eq(MqConst.FAIL).and(msDef.RETRY_COUNT.lt(maxRetryCount)))
                .orderBy(msDef.UPDATE_TIME.asc());
        // 表中有 text 字段, 因此先只查出 id, 再用 id 查具体的数据
        List<MqSend> sendList = Pages.returnList(mqSendMapper.paginate(Pages.paramOnlyLimit(limit), query));
        return A.isEmpty(sendList) ? Collections.emptyList() : mqSendMapper.selectListByIds(A.collect(sendList, MqSend::getId));
    }
}
