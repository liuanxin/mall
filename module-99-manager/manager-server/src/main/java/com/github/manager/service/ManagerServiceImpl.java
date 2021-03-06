package com.github.manager.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.common.encrypt.Encrypt;
import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.manager.model.*;
import com.github.manager.repository.*;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final ManagerUserMapper userMapper;
    private final ManagerRoleMapper roleMapper;
    private final ManagerMenuMapper menuMapper;
    private final ManagerPermissionMapper permissionMapper;

    private final ManagerUserRoleMapper userRoleMapper;
    private final ManagerRoleMenuMapper roleMenuMapper;
    private final ManagerRolePermissionMapper rolePermissionMapper;


    @Override
    public ManagerUser login(String userName, String password) {
        Wrapper<ManagerUser> query = Wrappers.lambdaQuery(ManagerUser.class).eq(ManagerUser::getUserName, userName);
        Page<ManagerUser> page = userMapper.selectPage(Pages.paramOnlyLimit(1), query);
        ManagerUser user = U.isNotBlank(page) ? A.first(page.getRecords()) : null;
        U.assertNil(user, "无此用户");
        return user;
    }

    @Override
    public ManagerUser getUser(Long userId) {
        U.assert0(userId, "没有这个用户");
        ManagerUser user = userMapper.selectById(userId);
        U.assertNil(user, "无此用户");
        return user;
    }

    @Override
    @Transactional
    public PageReturn<ManagerUser> queryUser(String userName, Boolean status, PageParam page) {
        Wrapper<ManagerUser> query = Wrappers.lambdaQuery(ManagerUser.class)
                .like(U.isNotBlank(userName), ManagerUser::getUserName, userName)
                .eq(U.isNotBlank(status), ManagerUser::getStatus, status);
        return Pages.returnPage(userMapper.selectPage(Pages.param(page), query));
    }

    @Override
    public void addOrUpdateUser(ManagerUser user) {
        Long userId = user.getId();
        if (U.greater0(userId)) {
            ManagerUser u = userMapper.selectById(userId);
            U.assertNil(u, "没有这个用户, 无法修改");

            if (U.isNotBlank(u.getPassword())) {
                U.assertException(U.isNotBlank(u.getHasManager()) && u.getHasManager(), "不能重置管理员密码, 请使用旧密码进行修改");
                U.assertException(U.isNotBlank(u.getStatus()) && u.getStatus(), "用户已被禁用, 请先解禁再修改密码");

                user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            }
            userMapper.updateById(user);
        } else {
            int count = userMapper.selectCount(Wrappers.lambdaQuery(ManagerUser.class)
                    .eq(ManagerUser::getUserName, user.getNickName()));
            U.assertException(count > 0, "已经有同名用户, 不能再次添加");

            user.setId(null);
            user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            userMapper.insert(user);
            userId = user.getId();
        }

        List<Long> rids = user.getRoleIds();
        if (A.isNotEmpty(rids)) {
            List<ManagerUserRole> userRoles = Lists.newArrayList();
            for (Long rid : rids) {
                if (U.greater0(rid)) {
                    userRoles.add(new ManagerUserRole().setUserId(userId).setRoleId(rid));
                }
            }
            if (A.isNotEmpty(userRoles)) {
                userRoleMapper.delete(Wrappers.lambdaQuery(ManagerUserRole.class).eq(ManagerUserRole::getUserId, userId));
                userRoleMapper.batchInsert(userRoles);
            }
        }
    }

    @Override
    public void deleteUser(Long id) {
        U.assert0(id, "无此用户");
        int flag = userMapper.deleteById(id);
        if (flag == 1) {
            userRoleMapper.delete(Wrappers.lambdaQuery(ManagerUserRole.class).eq(ManagerUserRole::getUserId, id));
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPass, String newPass) {
        U.assert0(userId, "无此用户");

        ManagerUser user = userMapper.selectById(userId);
        U.assertNil(user, "没有这个用户");
        U.assertException(U.isNotBlank(user.getStatus()) && user.getStatus(), "用户不能登录");
        U.assertException(Encrypt.checkNotBcrypt(oldPass, user.getPassword()), "旧密码有误");

        ManagerUser update = new ManagerUser();
        update.setId(userId);
        update.setPassword(Encrypt.bcryptEncode(newPass));
        userMapper.updateById(update);
    }


    @Override
    public List<ManagerRole> getUserRole(Long userId, boolean loadMenu, boolean loadPermission) {
        List<ManagerUserRole> userRoles = userRoleMapper.selectList(Wrappers.lambdaQuery(ManagerUserRole.class)
                .eq(ManagerUserRole::getUserId, userId));
        if (A.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        List<Long> rids = Lists.transform(userRoles, ManagerUserRole::getRoleId);
        if (A.isEmpty(rids)) {
            return Collections.emptyList();
        }

        Multimap<Long, ManagerMenu> menuMultiMap = HashMultimap.create();
        if (loadMenu) {
            List<ManagerRoleMenu> roleMenus = roleMenuMapper.selectList(Wrappers.lambdaQuery(ManagerRoleMenu.class)
                    .in(ManagerRoleMenu::getRoleId, rids));
            if (A.isNotEmpty(roleMenus)) {
                List<Long> mids = Lists.transform(roleMenus, ManagerRoleMenu::getMenuId);
                if (A.isNotEmpty(mids)) {
                    List<ManagerMenu> menus = menuMapper.selectList(Wrappers.lambdaQuery(ManagerMenu.class)
                            .in(ManagerMenu::getId, mids));

                    Map<Long, ManagerMenu> menuMap = Maps.uniqueIndex(menus, ManagerMenu::getId);
                    for (ManagerRoleMenu roleMenu : roleMenus) {
                        ManagerMenu menu = menuMap.get(roleMenu.getMenuId());
                        if (U.isNotBlank(menu)) {
                            menuMultiMap.put(roleMenu.getRoleId(), menu);
                        }
                    }
                }
            }
        }

        Multimap<Long, ManagerPermission> permissionMultimap = HashMultimap.create();
        if (loadPermission) {
            Wrapper<ManagerRolePermission> rolePermissionQuery = Wrappers.lambdaQuery(ManagerRolePermission.class)
                    .in(ManagerRolePermission::getRoleId, rids);
            List<ManagerRolePermission> rolePermissions = rolePermissionMapper.selectList(rolePermissionQuery);
            if (A.isNotEmpty(rolePermissions)) {
                List<Long> pids = Lists.transform(rolePermissions, ManagerRolePermission::getPermissionId);
                if (A.isNotEmpty(pids)) {
                    Wrapper<ManagerPermission> permissionQuery = Wrappers.lambdaQuery(ManagerPermission.class)
                            .in(ManagerPermission::getId, pids);
                    List<ManagerPermission> permissions = permissionMapper.selectList(permissionQuery);

                    Map<Long, ManagerPermission> permissionMap = Maps.uniqueIndex(permissions, ManagerPermission::getId);
                    for (ManagerRolePermission rolePermission : rolePermissions) {
                        ManagerPermission permission = permissionMap.get(rolePermission.getPermissionId());
                        if (U.isNotBlank(permission)) {
                            permissionMultimap.put(rolePermission.getPermissionId(), permission);
                        }
                    }
                }
            }
        }

        Wrapper<ManagerRole> roleQuery = Wrappers.lambdaQuery(ManagerRole.class).in(ManagerRole::getId, rids);
        List<ManagerRole> roles = roleMapper.selectList(roleQuery);
        if (A.isEmpty(roles)) {
            return Collections.emptyList();
        }

        for (ManagerRole role : roles) {
            Long rid = role.getId();

            if (A.isNotEmpty(menuMultiMap)) {
                Collection<ManagerMenu> managerMenus = menuMultiMap.get(rid);
                if (A.isNotEmpty(managerMenus)) {
                    role.setMenus(Lists.newArrayList(managerMenus));
                }
            }

            if (A.isNotEmpty(permissionMultimap)) {
                Collection<ManagerPermission> managerPermissions = permissionMultimap.get(rid);
                if (A.isNotEmpty(managerPermissions)) {
                    role.setPermissions(Lists.newArrayList(managerPermissions));
                }
            }
        }
        return roles;
    }

    @Override
    public List<ManagerRole> queryBasicRole() {
        return roleMapper.selectList(null);
    }

    @Override
    @Transactional
    public void addOrUpdateRole(ManagerRole role) {
        Long rid = role.getId();
        if (U.greater0(rid)) {
            int count = roleMapper.selectCount(Wrappers.lambdaQuery(ManagerRole.class).eq(ManagerRole::getId, rid));
            U.assertException(count == 0, "没有这个角色, 无法修改");

            roleMapper.updateById(role);
        } else {
            int count = roleMapper.selectCount(Wrappers.lambdaQuery(ManagerRole.class)
                    .eq(ManagerRole::getName, role.getName()));
            U.assertException(count > 0, "已经有同名角色, 不能再次添加");

            role.setId(null);
            roleMapper.insert(role);
            rid = role.getId();
        }

        List<Long> mids = role.getMenuIds();
        if (A.isNotEmpty(mids)) {
            List<ManagerRoleMenu> roleMenus = Lists.newArrayList();
            for (Long mid : mids) {
                if (U.greater0(mid)) {
                    roleMenus.add(new ManagerRoleMenu().setRoleId(rid).setMenuId(mid));
                }
            }
            if (A.isNotEmpty(roleMenus)) {
                roleMenuMapper.delete(Wrappers.lambdaQuery(ManagerRoleMenu.class).eq(ManagerRoleMenu::getRoleId, rid));
                roleMenuMapper.batchInsert(roleMenus);
            }
        }

        List<Long> pids = role.getPermissionIds();
        if (A.isNotEmpty(pids)) {
            List<ManagerRolePermission> rolePermissions = Lists.newArrayList();
            for (Long pid : pids) {
                if (U.greater0(pid)) {
                    rolePermissions.add(new ManagerRolePermission().setRoleId(rid).setPermissionId(pid));
                }
            }
            if (A.isNotEmpty(rolePermissions)) {
                rolePermissionMapper.delete(Wrappers.lambdaQuery(ManagerRolePermission.class)
                        .eq(ManagerRolePermission::getRoleId, rid));
                rolePermissionMapper.batchInsert(rolePermissions);
            }
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        U.assert0(roleId, "无此角色");

        int count = userRoleMapper.selectCount(Wrappers.lambdaQuery(ManagerUserRole.class)
                .eq(ManagerUserRole::getRoleId, roleId));
        U.assertException(count > 0, "有用户分配了此角色, 请先取消分配再删除");

        int flag = roleMapper.deleteById(roleId);
        if (flag == 1) {
            roleMenuMapper.delete(Wrappers.lambdaQuery(ManagerRoleMenu.class)
                    .eq(ManagerRoleMenu::getRoleId, roleId));
            rolePermissionMapper.delete(Wrappers.lambdaQuery(ManagerRolePermission.class)
                    .eq(ManagerRolePermission::getRoleId, roleId));
        }
    }


    @Override
    public List<ManagerMenu> queryMenu(String name) {
        return menuMapper.selectList(Wrappers.lambdaQuery(ManagerMenu.class)
                .like(U.isNotBlank(name), ManagerMenu::getName, name));
    }

    @Override
    public void addOrUpdateMenu(ManagerMenu menu) {
        Long mid = menu.getId();
        if (U.greater0(mid)) {
            ManagerMenu m = menuMapper.selectById(mid);
            U.assertNil(m, "没有这个菜单, 无法修改");

            menuMapper.updateById(menu);
        } else {
            int count = menuMapper.selectCount(Wrappers.lambdaQuery(ManagerMenu.class)
                    .eq(ManagerMenu::getName, menu.getName()));
            U.assertException(count > 0, "已经有同名菜单, 不能再次添加");

            menu.setId(null);
            menuMapper.insert(menu);
        }
    }

    @Override
    public void deleteMenu(Long menuId) {
        U.assert0(menuId, "无此菜单");

        int count = permissionMapper.selectCount(Wrappers.lambdaQuery(ManagerPermission.class)
                .eq(ManagerPermission::getMenuId, menuId));
        U.assertException(count > 0, "此菜单下已经有权限了, 请先将权限删除再来删除菜单");

        menuMapper.deleteById(menuId);
    }

    @Override
    public void deleteMenus(List<Long> mids) {
        if (A.isNotEmpty(mids)) {
            int count = permissionMapper.selectCount(Wrappers.lambdaQuery(ManagerPermission.class)
                    .in(ManagerPermission::getMenuId, mids));
            U.assertException(count > 0, "传入的菜单下已经有权限了, 请先将权限删除再来删除菜单");

            menuMapper.delete(Wrappers.lambdaQuery(ManagerMenu.class).in(ManagerMenu::getId, mids));
        }
    }


    @Override
    public List<ManagerPermission> queryPermission(String name) {
        return permissionMapper.selectList(Wrappers.lambdaQuery(ManagerPermission.class)
                .like(U.isNotBlank(name), ManagerPermission::getName, name));
    }

    @Override
    public void addOrUpdatePermission(ManagerPermission permission) {
        Long pid = permission.getId();
        if (U.greater0(pid)) {
            ManagerPermission p = permissionMapper.selectById(pid);
            U.assertNil(p, "没有这个权限, 无法修改");

            permissionMapper.updateById(permission);
        } else {
            int count = permissionMapper.selectCount(Wrappers.lambdaQuery(ManagerPermission.class)
                    .eq(ManagerPermission::getMethod, permission.getMethod())
                    .eq(ManagerPermission::getUrl, permission.getUrl())
            );
            U.assertException(count > 0, "已经有同样规则的权限, 不能再次添加");

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
            permissionMapper.delete(Wrappers.lambdaQuery(ManagerPermission.class).in(ManagerPermission::getId, pids));
        }
    }
}
