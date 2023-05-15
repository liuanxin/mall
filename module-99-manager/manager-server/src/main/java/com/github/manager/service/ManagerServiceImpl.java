package com.github.manager.service;

import com.github.common.encrypt.Encrypt;
import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.manager.model.*;
import com.github.manager.model.table.Tables;
import com.github.manager.repository.*;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final ManagerUserMapper userMapper;
    private final ManagerRoleMapper roleMapper;
    private final ManagerMenuMapper menuMapper;
    private final ManagerPermissionMapper permissionMapper;

    private final ManagerUserRoleMapper userRoleMapper;
    private final ManagerRoleMenuMapper roleMenuMapper;
    private final ManagerRolePermissionMapper rolePermissionMapper;


    @Override
    public ManagerUser getUser(String userName) {
        QueryWrapper query = QueryWrapper.create();
        query.and(Tables.MANAGER_USER.USER_NAME.eq(userName));
        Page<ManagerUser> page = userMapper.paginate(Pages.paramOnlyLimit(1), query);
        return Pages.returnOne(page);
    }

    @Override
    public ManagerUser getUser(Long userId) {
        U.assert0(userId, "没有这个用户");
        ManagerUser user = userMapper.selectOneById(userId);
        U.assertNil(user, "无此用户");
        return user;
    }

    @Override
    @Transactional
    public PageReturn<ManagerUser> queryUser(String userName, Boolean status, PageParam page) {
        QueryWrapper query = QueryWrapper.create();
        if (U.isNotBlank(userName)) {
            query.and(Tables.MANAGER_USER.USER_NAME.like(userName));
        }
        if (U.isNotNull(status)) {
            query.and(Tables.MANAGER_USER.STATUS.eq(status));
        }
        return Pages.returnPage(userMapper.paginate(Pages.param(page), query));
    }

    @Override
    public void addOrUpdateUser(ManagerUser user) {
        Long userId = user.getId();
        if (U.greater0(userId)) {
            ManagerUser u = userMapper.selectOneById(userId);
            U.assertNil(u, "没有这个用户, 无法修改");

            if (U.isNotBlank(u.getPassword())) {
                U.assertException(U.isNotNull(u.getHasManager()) && u.getHasManager(), "不能重置管理员密码, 请使用旧密码进行修改");
                U.assertException(U.isNotNull(u.getStatus()) && u.getStatus(), "用户已被禁用, 请先解禁再修改密码");

                user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            }
            userMapper.update(user);
        } else {
            QueryWrapper query = QueryWrapper.create();
            query.select(Tables.MANAGER_USER.ID).and(Tables.MANAGER_USER.USER_NAME.eq(user.getNickName()));
            boolean exists = Pages.hasExists(userMapper.paginate(Pages.paramOnlyLimit(1), query));
            U.assertException(exists, "已经有同名用户, 不能再次添加");

            user.setId(null);
            user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            userMapper.insert(user);
            userId = user.getId();
        }

        List<Long> rids = user.getRoleIds();
        if (A.isNotEmpty(rids)) {
            List<ManagerUserRole> userRoles = new ArrayList<>();
            for (Long rid : rids) {
                if (U.greater0(rid)) {
                    userRoles.add(new ManagerUserRole().setUserId(userId).setRoleId(rid));
                }
            }
            if (A.isNotEmpty(userRoles)) {
                userRoleMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_USER_ROLE.USER_ID.eq(userId)));
                userRoleMapper.insertBatch(userRoles);
            }
        }
    }

    @Override
    public void deleteUser(Long id) {
        U.assert0(id, "无此用户");
        int flag = userMapper.deleteById(id);
        if (flag == 1) {
            userRoleMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_USER_ROLE.USER_ID.eq(id)));
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPass, String newPass) {
        U.assert0(userId, "无此用户");

        ManagerUser user = userMapper.selectOneById(userId);
        U.assertNil(user, "没有这个用户");
        U.assertException(U.isNotNull(user.getStatus()) && user.getStatus(), "用户不能登录");
        U.assertException(Encrypt.checkNotBcrypt(oldPass, user.getPassword()), "旧密码有误");

        ManagerUser update = new ManagerUser();
        update.setId(userId);
        update.setPassword(Encrypt.bcryptEncode(newPass));
        userMapper.update(update);
    }


    @Override
    public List<ManagerRole> getUserRole(Long userId, boolean loadMenu, boolean loadPermission) {
        QueryWrapper query = QueryWrapper.create().and(Tables.MANAGER_USER_ROLE.USER_ID.eq(userId));
        List<ManagerUserRole> userRoles = userRoleMapper.selectListByQuery(query);
        if (A.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        List<Long> rids = A.collect(userRoles, ManagerUserRole::getRoleId);
        if (A.isEmpty(rids)) {
            return Collections.emptyList();
        }

        Map<Long, List<ManagerMenu>> menuMultiMap = new HashMap<>();
        if (loadMenu) {
            List<ManagerRoleMenu> roleMenus = roleMenuMapper.selectListByQuery(QueryWrapper.create()
                    .and(Tables.MANAGER_ROLE_MENU.ROLE_ID.in(rids)));
            if (A.isNotEmpty(roleMenus)) {
                List<Long> ids = A.collect(roleMenus, ManagerRoleMenu::getMenuId);
                if (A.isNotEmpty(ids)) {
                    List<ManagerMenu> menus = menuMapper.selectListByQuery(QueryWrapper.create()
                            .and(Tables.MANAGER_MENU.ID.in(ids)));
                    Map<Long, ManagerMenu> menuMap = A.listToMap(menus, ManagerMenu::getId);
                    for (ManagerRoleMenu roleMenu : roleMenus) {
                        ManagerMenu menu = menuMap.get(roleMenu.getMenuId());
                        if (U.isNotNull(menu)) {
                            menuMultiMap.computeIfAbsent(roleMenu.getRoleId(), (k1) -> new ArrayList<>()).add(menu);
                        }
                    }
                }
            }
        }

        Map<Long, List<ManagerPermission>> permissionMultiMap = new HashMap<>();
        if (loadPermission) {
            QueryWrapper rolePermissionQuery = QueryWrapper.create().and(Tables.MANAGER_ROLE_PERMISSION.ROLE_ID.in(rids));
            List<ManagerRolePermission> rolePermissions = rolePermissionMapper.selectListByQuery(rolePermissionQuery);
            if (A.isNotEmpty(rolePermissions)) {
                List<Long> ids = A.collect(rolePermissions, ManagerRolePermission::getPermissionId);
                if (A.isNotEmpty(ids)) {
                    QueryWrapper permissionQuery = QueryWrapper.create().and(Tables.MANAGER_PERMISSION.ID.in(ids));
                    List<ManagerPermission> permissions = permissionMapper.selectListByQuery(permissionQuery);

                    Map<Long, ManagerPermission> permissionMap = A.listToMap(permissions, ManagerPermission::getId);
                    for (ManagerRolePermission rolePermission : rolePermissions) {
                        Long permissionId = rolePermission.getPermissionId();
                        ManagerPermission permission = permissionMap.get(permissionId);
                        if (U.isNotNull(permission)) {
                            permissionMultiMap.computeIfAbsent(permissionId, (k1) -> new ArrayList<>()).add(permission);
                        }
                    }
                }
            }
        }

        QueryWrapper roleQuery = QueryWrapper.create().and(Tables.MANAGER_ROLE.ID.in(rids));
        List<ManagerRole> roles = roleMapper.selectListByQuery(roleQuery);
        if (A.isEmpty(roles)) {
            return Collections.emptyList();
        }

        for (ManagerRole role : roles) {
            Long rid = role.getId();

            List<ManagerMenu> managerMenus = menuMultiMap.get(rid);
            if (A.isNotEmpty(managerMenus)) {
                role.setMenus(managerMenus);
            }

            List<ManagerPermission> managerPermissions = permissionMultiMap.get(rid);
            if (A.isNotEmpty(managerPermissions)) {
                role.setPermissions(managerPermissions);
            }
        }
        return roles;
    }

    @Override
    public List<ManagerRole> queryBasicRole() {
        return roleMapper.selectAll();
    }

    @Override
    @Transactional
    public void addOrUpdateRole(ManagerRole role) {
        Long rid = role.getId();
        if (U.greater0(rid)) {
            QueryWrapper existsQuery = QueryWrapper.create()
                    .select(Tables.MANAGER_ROLE.ID).and(Tables.MANAGER_ROLE.ID.eq(rid));
            boolean hasNotExists = Pages.hasNotExists(roleMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
            U.assertException(hasNotExists, "没有这个角色, 无法修改");

            roleMapper.update(role);
        } else {
            QueryWrapper existsQuery = QueryWrapper.create()
                    .select(Tables.MANAGER_ROLE.ID).and(Tables.MANAGER_ROLE.NAME.eq(role.getName()));
            boolean exists = Pages.hasExists(roleMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
            U.assertException(exists, "已经有同名角色, 不能再次添加");

            role.setId(null);
            roleMapper.insert(role);
            rid = role.getId();
        }

        List<Long> mids = role.getMenuIds();
        if (A.isNotEmpty(mids)) {
            List<ManagerRoleMenu> roleMenus = new ArrayList<>();
            for (Long mid : mids) {
                if (U.greater0(mid)) {
                    roleMenus.add(new ManagerRoleMenu().setRoleId(rid).setMenuId(mid));
                }
            }
            if (A.isNotEmpty(roleMenus)) {
                roleMenuMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_ROLE_MENU.ROLE_ID.eq(rid)));
                roleMenuMapper.batchInsert(roleMenus);
            }
        }

        List<Long> pids = role.getPermissionIds();
        if (A.isNotEmpty(pids)) {
            List<ManagerRolePermission> rolePermissions = new ArrayList<>();
            for (Long pid : pids) {
                if (U.greater0(pid)) {
                    rolePermissions.add(new ManagerRolePermission().setRoleId(rid).setPermissionId(pid));
                }
            }
            if (A.isNotEmpty(rolePermissions)) {
                rolePermissionMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_ROLE_PERMISSION.ROLE_ID.eq(rid)));
                rolePermissionMapper.batchInsert(rolePermissions);
            }
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        U.assert0(roleId, "无此角色");

        QueryWrapper existsQuery = QueryWrapper.create()
                .select(Tables.MANAGER_USER_ROLE.ID).and(Tables.MANAGER_USER_ROLE.ROLE_ID.eq(roleId));
        boolean exists = Pages.hasExists(userRoleMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
        U.assertException(exists, "已经有用户分配了这个角色, 请先取消分配再删除");

        int flag = roleMapper.deleteById(roleId);
        if (flag == 1) {
            roleMenuMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_ROLE_MENU.ROLE_ID.eq(roleId)));
            rolePermissionMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_ROLE_PERMISSION.ROLE_ID.eq(roleId)));
        }
    }


    @Override
    public List<ManagerMenu> queryMenu(String name) {
        QueryWrapper query = QueryWrapper.create();
        if (U.isNotBlank(name)) {
            query.and(Tables.MANAGER_MENU.NAME.like(name));
        }
        return menuMapper.selectListByQuery(query);
    }

    @Override
    public void addOrUpdateMenu(ManagerMenu menu) {
        Long mid = menu.getId();
        if (U.greater0(mid)) {
            ManagerMenu m = menuMapper.selectOneById(mid);
            U.assertNil(m, "没有这个菜单, 无法修改");

            menuMapper.update(menu);
        } else {
            QueryWrapper existsQuery = QueryWrapper.create().select(Tables.MANAGER_MENU.ID)
                    .where(Tables.MANAGER_MENU.NAME.eq(menu.getName()));
            boolean exists = Pages.hasExists(menuMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
            U.assertException(exists, "已经有同名菜单, 不能再次添加");

            menu.setId(null);
            menuMapper.insert(menu);
        }
    }

    @Override
    public void deleteMenu(Long menuId) {
        U.assert0(menuId, "无此菜单");

        QueryWrapper existsQuery = QueryWrapper.create()
                .select(Tables.MANAGER_PERMISSION.ID).and(Tables.MANAGER_PERMISSION.MENU_ID.eq(menuId));
        boolean exists = Pages.hasExists(permissionMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
        U.assertException(exists, "此菜单下已经有权限了, 请先将权限删除再来删除菜单");

        menuMapper.deleteById(menuId);
    }

    @Override
    public void deleteMenus(List<Long> mids) {
        if (A.isNotEmpty(mids)) {
            QueryWrapper existsQuery = QueryWrapper.create()
                    .select(Tables.MANAGER_PERMISSION.ID)
                    .and(Tables.MANAGER_PERMISSION.MENU_ID.in(mids));
            boolean exists = Pages.hasExists(permissionMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
            U.assertException(exists, "传入的菜单下已经有权限了, 请先将权限删除再来删除菜单");

            menuMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_MENU.ID.in(mids)));
        }
    }


    @Override
    public List<ManagerPermission> queryPermission(String name) {
        QueryWrapper query = QueryWrapper.create();
        if (U.isNotBlank(name)) {
            query.and(Tables.MANAGER_PERMISSION.NAME.like(name));
        }
        return permissionMapper.selectListByQuery(query);
    }

    @Override
    public void addOrUpdatePermission(ManagerPermission permission) {
        Long pid = permission.getId();
        if (U.greater0(pid)) {
            ManagerPermission p = permissionMapper.selectOneById(pid);
            U.assertNil(p, "没有这个权限, 无法修改");

            permissionMapper.update(permission);
        } else {
            QueryWrapper existsQuery = QueryWrapper.create()
                    .select(Tables.MANAGER_PERMISSION.ID)
                    .and(Tables.MANAGER_PERMISSION.METHOD.eq(permission.getMethod()))
                    .and(Tables.MANAGER_PERMISSION.URL.eq(permission.getUrl()));
            boolean exists = Pages.hasExists(permissionMapper.paginate(Pages.paramOnlyLimit(1), existsQuery));
            U.assertException(exists, "已经有同样规则的权限, 不能再次添加");

            permission.setId(null);
            permissionMapper.insert(permission);
        }
    }

    @Override
    public void deletePermission(Long permissionId) {
        permissionMapper.deleteById(permissionId);
    }

    @Override
    public void deletePermissions(List<Long> pids) {
        if (A.isNotEmpty(pids)) {
            permissionMapper.deleteByQuery(QueryWrapper.create().and(Tables.MANAGER_PERMISSION.ID.in(pids)));
        }
    }
}
