package com.github.manager.service;

import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 管理模块的接口实现类
 */
@Service
public class ManagerServiceImpl implements ManagerService {

    @Override
    public PageInfo demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用实现类" + xx + ", page:" + page + ", limit:" + limit);
        }
        return Pages.returnPage(null);
    }
}
