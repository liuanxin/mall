package com.github.util;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
class ManagerSessionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 默认未登录用户的 id */
    private static final Long DEFAULT_ID = 0L;
    /** 默认未登录用户的 name */
    private static final String DEFAULT_NAME = "未登录用户";
    /** 超级管理员账号 */
    private static final List<String> SUPER_USER = Arrays.asList("admin", "root");
    /** session 中无存放对象时的默认对象 */
    private static final ManagerSessionModel DEFAULT_MODEL = new ManagerSessionModel(DEFAULT_ID, DEFAULT_NAME);


    // ========== 存放在 session 中的数据 ==========

    /** 用户 id */
    private Long id;
    /** 用户名 */
    private String userName;
    /** 权限列表 */
    private List<Permission> permissionList;

    // ========== 存放在 session 中的数据 ==========

    private ManagerSessionModel(Long id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public String userInfo() {
        return U.toStr(id) + "/"  + U.toStr(userName);
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Resource implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 菜单名 */
        private String name;
        /** 菜单样式 */
        private String clazz;
        /** 子菜单 */
        private List<Resource> childList;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Permission implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 权限路径 */
        private String url;
        /** 权限方法, 包括(get,head,post,put,delete)五种, 多个用逗号隔开 */
        private String method;
    }


    private boolean wasLogin() {
        return !DEFAULT_ID.equals(id) && !DEFAULT_NAME.equals(userName);
    }
    boolean notLogin() {
        return !wasLogin();
    }

    boolean hasAdmin() {
        return SUPER_USER.contains(userName);
    }
    boolean notAdmin() {
        return !hasAdmin();
    }

    /** 有访问权限就返回 true */
    private boolean wasPermission(String url, String method) {
        if (A.isNotEmpty(permissionList)) {
            for (Permission permission : permissionList) {
                String permissionUrl = permission.getUrl();
                String permissionMethod = permission.getMethod();

                // 如果配置的 url 是 /user/*, 传进来的是 /user/info 也可以通过, 通配 或 全字
                boolean matchUrl = permissionUrl.endsWith("/*") && url.startsWith(permissionUrl.replace("*", ""));
                boolean urlCheck = matchUrl || url.equals(permissionUrl);

                // 如果配置的 method 是 *, 传进来的是 GET 也可以通过, 通配 或 全字
                boolean methodCheck = (("*").equals(permissionMethod) || permissionMethod.contains(method));

                // url 和 method 都通过才表示有访问权限
                if (urlCheck && methodCheck) {
                    return true;
                }
            }
        }
        return false;
    }
    /** 无访问权限就返回 true */
    boolean notPermission(String url, String method) {
        return !wasPermission(url, method);
    }


    // 以下为静态方法


    /** 将数据库对应的数据模型转换成 session 中存放的对象 */
    static <T,P> ManagerSessionModel assemblyData(T account, List<P> permissions) {
        ManagerSessionModel sessionModel = JsonUtil.convert(account, ManagerSessionModel.class);
        if (U.isNotNull(sessionModel)) {
            List<Permission> permissionList = JsonUtil.convertList(permissions, Permission.class);
            if (A.isNotEmpty(permissionList)) {
                sessionModel.setPermissionList(permissionList);
            }
        }
        return sessionModel;
    }

    static ManagerSessionModel defaultUser() {
        return DEFAULT_MODEL;
    }
}
