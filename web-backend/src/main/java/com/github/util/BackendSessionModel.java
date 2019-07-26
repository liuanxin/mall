package com.github.util;

import com.github.common.json.JsonUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
class BackendSessionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 默认未登录用户的 id */
    private static final Long DEFAULT_ID = 0L;
    /** 默认未登录用户的 name */
    private static final String DEFAULT_NAME = "未登录用户";
    private static final BackendSessionModel DEFAULT_MODEL = new BackendSessionModel(DEFAULT_ID, DEFAULT_NAME);


    // ========== 存放在 session 中的数据 ==========

    /** 存放在 session 中的(用户 id) */
    private Long id;
    /** 存放在 session 中的(用户名) */
    private String name;

    // ========== 存放在 session 中的数据 ==========

    private BackendSessionModel(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    /** 当前用户在指定域名下是否已登录. 已登录就返回 true */
    boolean wasLogin() {
        return !Objects.equals(DEFAULT_ID, id) && !Objects.equals(DEFAULT_NAME, name);
    }


    // 以下为静态方法


    /** 组装数据, 将用户对象跟存进 session 中的数据进行转换 */
    static <T> BackendSessionModel assemblyData(T user) {
        // user 跟 sessionModel 的属性一一对应就可以, 如果不对应就像下面这样一行一行写了
        // BackendSessionModel sessionModel = new BackendSessionModel();
        // sessionModel.setId(user.getXXId());
        // sessionModel.setName(user.getXXName());
        return JsonUtil.convert(user, BackendSessionModel.class);
    }

    /** 未登录时的默认用户信息 */
    static BackendSessionModel defaultUser() {
        return DEFAULT_MODEL;
    }
}
