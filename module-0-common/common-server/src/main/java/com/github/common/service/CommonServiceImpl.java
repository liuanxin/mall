package com.github.common.service;

import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import org.springframework.stereotype.Service;

/**
 * 公共模块的接口实现类
 */
@Service
public class CommonServiceImpl implements CommonService {

    @Override
    public PageInfo demo(String xx, Page page) {
        return Pages.returnPage(null);
    }
}
