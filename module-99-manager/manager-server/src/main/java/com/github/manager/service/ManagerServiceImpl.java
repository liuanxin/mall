package com.github.manager.service;

import com.github.common.encrypt.Encrypt;
import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.liuanxin.page.model.PageBounds;
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
        ManagerUserExample userExample = new ManagerUserExample();
        userExample.or().andUserNameEqualTo(userName);
        ManagerUser user = A.first(userMapper.selectByExample(userExample, new PageBounds(1)));

        U.assertNil(user, "无此用户");
        return user;
    }

    @Override
    public ManagerUser getUser(Long userId) {
        U.assert0(userId, "没有这个用户");
        ManagerUser user = userMapper.selectByPrimaryKey(userId);
        U.assertNil(user, "无此用户");
        return user;
    }

    @Override
    public PageInfo<ManagerUser> queryUser(String userName, Boolean status, Page page) {
        ManagerUserExample userExample = new ManagerUserExample();
        ManagerUserExample.Criteria or = userExample.or();
        if (U.isNotBlank(userName)) {
            or.andUserNameLike(U.like(userName));
        }
        if (U.isNotBlank(status)) {
            or.andStatusEqualTo(status);
        }
        return Pages.returnPage(userMapper.selectByExample(userExample, Pages.param(page)));
    }

    @Override
    public void addOrUpdateUser(ManagerUser user) {
        Long uid = user.getId();
        if (U.greater0(uid)) {
            ManagerUser u = userMapper.selectByPrimaryKey(uid);
            U.assertNil(u, "没有这个用户, 无法修改");

            if (U.isNotBlank(u.getPassword())) {
                U.assertException(U.isNotBlank(u.getHasManager()) && u.getHasManager(), "不能重置管理员密码, 请使用旧密码进行修改");
                U.assertException(U.isNotBlank(u.getStatus()) && u.getStatus(), "用户已被禁用, 请先解禁再修改密码");

                user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            }
            userMapper.updateByPrimaryKeySelective(user);
        } else {
            ManagerUserExample userExample = new ManagerUserExample();
            userExample.or().andUserNameEqualTo(user.getUserName());
            long count = userMapper.countByExample(userExample);
            U.assertException(count > 0, "已经有同名用户, 不能再次添加");

            user.setId(null);
            user.setPassword(Encrypt.bcryptEncode(user.getPassword()));
            userMapper.insertSelective(user);
            uid = user.getId();
        }

        List<Long> rids = user.getRoleIds();
        if (A.isNotEmpty(rids)) {
            List<ManagerUserRole> userRoles = Lists.newArrayList();
            for (Long rid : rids) {
                if (U.greater0(rid)) {
                    userRoles.add(new ManagerUserRole(uid, rid));
                }
            }
            if (A.isNotEmpty(userRoles)) {
                ManagerUserRoleExample userRoleExample = new ManagerUserRoleExample();
                userRoleExample.or().andUserIdEqualTo(uid);
                userRoleMapper.deleteByExample(userRoleExample);
                userRoleMapper.batchInsert(userRoles);
            }
        }
    }

    @Override
    public void deleteUser(Long id) {
        U.assert0(id, "无此用户");
        int flag = userMapper.deleteByPrimaryKey(id);
        if (flag == 1) {
            ManagerUserRoleExample userRoleExample = new ManagerUserRoleExample();
            userRoleExample.or().andUserIdEqualTo(id);
            userRoleMapper.deleteByExample(userRoleExample);
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPass, String newPass) {
        U.assert0(userId, "无此用户");

        ManagerUser user = userMapper.selectByPrimaryKey(userId);
        U.assertNil(user, "没有这个用户");
        U.assertException(U.isNotBlank(user.getStatus()) && user.getStatus(), "用户不能登录");
        U.assertException(Encrypt.checkNotBcrypt(oldPass, user.getPassword()), "旧密码有误");

        ManagerUser update = new ManagerUser();
        update.setId(userId);
        update.setPassword(Encrypt.bcryptEncode(newPass));
        userMapper.updateByPrimaryKeySelective(update);
    }


    @Override
    public List<ManagerRole> getUserRole(Long userId, boolean loadMenu, boolean loadPermission) {
        ManagerUserRoleExample userRoleExample = new ManagerUserRoleExample();
        userRoleExample.or().andUserIdEqualTo(userId);
        List<ManagerUserRole> userRoles = userRoleMapper.selectByExample(userRoleExample);
        if (A.isEmpty(userRoles)) {
            return Collections.emptyList();
        }
        List<Long> rids = Lists.transform(userRoles, ManagerUserRole::getRoleId);
        if (A.isEmpty(rids)) {
            return Collections.emptyList();
        }

        Multimap<Long, ManagerMenu> menuMultiMap = HashMultimap.create();
        if (loadMenu) {
            ManagerRoleMenuExample roleMenuExample = new ManagerRoleMenuExample();
            roleMenuExample.or().andRoleIdIn(rids);
            List<ManagerRoleMenu> roleMenus = roleMenuMapper.selectByExample(roleMenuExample);
            if (A.isNotEmpty(roleMenus)) {
                List<Long> mids = Lists.transform(roleMenus, ManagerRoleMenu::getMenuId);
                if (A.isNotEmpty(mids)) {
                    ManagerMenuExample menuExample = new ManagerMenuExample();
                    menuExample.or().andIdIn(mids);
                    List<ManagerMenu> menus = menuMapper.selectByExample(menuExample);

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
            ManagerRolePermissionExample rolePermissionExample = new ManagerRolePermissionExample();
            rolePermissionExample.or().andRoleIdIn(rids);
            List<ManagerRolePermission> rolePermissions = rolePermissionMapper.selectByExample(rolePermissionExample);
            if (A.isNotEmpty(rolePermissions)) {
                List<Long> pids = Lists.transform(rolePermissions, ManagerRolePermission::getPermissionId);
                if (A.isNotEmpty(pids)) {
                    ManagerPermissionExample permissionExample = new ManagerPermissionExample();
                    permissionExample.or().andIdIn(pids);
                    List<ManagerPermission> permissions = permissionMapper.selectByExample(permissionExample);

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

        ManagerRoleExample roleExample = new ManagerRoleExample();
        roleExample.or().andIdIn(rids);
        List<ManagerRole> roles = roleMapper.selectByExample(roleExample);
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
        return roleMapper.selectByExample(null);
    }

    @Override
    @Transactional
    public void addOrUpdateRole(ManagerRole role) {
        Long rid = role.getId();
        if (U.greater0(rid)) {
            ManagerRoleExample roleExample = new ManagerRoleExample();
            roleExample.or().andIdEqualTo(rid);
            long count = roleMapper.countByExample(roleExample);
            U.assertException(count == 0, "没有这个角色, 无法修改");

            roleMapper.updateByPrimaryKeySelective(role);
        } else {
            ManagerRoleExample roleExample = new ManagerRoleExample();
            roleExample.or().andNameEqualTo(role.getName());
            long count = roleMapper.countByExample(roleExample);
            U.assertException(count > 0, "已经有同名角色, 不能再次添加");

            role.setId(null);
            roleMapper.insertSelective(role);
            rid = role.getId();
        }

        List<Long> mids = role.getMenuIds();
        if (A.isNotEmpty(mids)) {
            List<ManagerRoleMenu> roleMenus = Lists.newArrayList();
            for (Long mid : mids) {
                if (U.greater0(mid)) {
                    roleMenus.add(new ManagerRoleMenu(rid, mid));
                }
            }
            if (A.isNotEmpty(roleMenus)) {
                ManagerRoleMenuExample roleMenuExample = new ManagerRoleMenuExample();
                roleMenuExample.or().andRoleIdEqualTo(rid);
                roleMenuMapper.deleteByExample(roleMenuExample);
                roleMenuMapper.batchInsert(roleMenus);
            }
        }

        List<Long> pids = role.getPermissionIds();
        if (A.isNotEmpty(pids)) {
            List<ManagerRolePermission> rolePermissions = Lists.newArrayList();
            for (Long pid : pids) {
                if (U.greater0(pid)) {
                    rolePermissions.add(new ManagerRolePermission(rid, pid));
                }
            }
            if (A.isNotEmpty(rolePermissions)) {
                ManagerRolePermissionExample rolePermissionExample = new ManagerRolePermissionExample();
                rolePermissionExample.or().andRoleIdEqualTo(rid);
                rolePermissionMapper.deleteByExample(rolePermissionExample);
                rolePermissionMapper.batchInsert(rolePermissions);
            }
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        U.assert0(roleId, "无此角色");

        ManagerUserRoleExample userRoleExample = new ManagerUserRoleExample();
        userRoleExample.or().andRoleIdEqualTo(roleId);
        long count = userRoleMapper.countByExample(userRoleExample);
        U.assertException(count > 0, "有用户分配了此角色, 请先取消分配再删除");

        int flag = roleMapper.deleteByPrimaryKey(roleId);
        if (flag == 1) {
            ManagerRoleMenuExample roleMenuExample = new ManagerRoleMenuExample();
            roleMenuExample.or().andRoleIdEqualTo(roleId);
            roleMenuMapper.deleteByExample(roleMenuExample);

            ManagerRolePermissionExample rolePermissionExample = new ManagerRolePermissionExample();
            rolePermissionExample.or().andRoleIdEqualTo(roleId);
            rolePermissionMapper.deleteByExample(rolePermissionExample);
        }
    }


    @Override
    public List<ManagerMenu> queryMenu(String name) {
        ManagerMenuExample menuExample = new ManagerMenuExample();
        if (U.isNotBlank(name)) {
            menuExample.or().andNameLike(U.like(name));
        }
        return menuMapper.selectByExample(menuExample);
    }

    @Override
    public void addOrUpdateMenu(ManagerMenu menu) {
        Long mid = menu.getId();
        if (U.greater0(mid)) {
            ManagerMenu m = menuMapper.selectByPrimaryKey(mid);
            U.assertNil(m, "没有这个菜单, 无法修改");

            menuMapper.updateByPrimaryKeySelective(menu);
        } else {
            ManagerMenuExample menuExample = new ManagerMenuExample();
            menuExample.or().andNameEqualTo(menu.getName());
            long count = menuMapper.countByExample(menuExample);
            U.assertException(count > 0, "已经有同名菜单, 不能再次添加");

            menu.setId(null);
            menuMapper.insertSelective(menu);
        }
    }

    @Override
    public void deleteMenu(Long menuId) {
        U.assert0(menuId, "无此菜单");

        ManagerPermissionExample permissionExample = new ManagerPermissionExample();
        permissionExample.or().andMenuIdEqualTo(menuId);
        long count = permissionMapper.countByExample(permissionExample);
        U.assertException(count > 0, "此菜单下已经有权限了, 请先将权限删除再来删除菜单");

        menuMapper.deleteByPrimaryKey(menuId);
    }

    @Override
    public void deleteMenus(List<Long> mids) {
        if (A.isNotEmpty(mids)) {
            ManagerPermissionExample permissionExample = new ManagerPermissionExample();
            permissionExample.or().andMenuIdIn(mids);
            long count = permissionMapper.countByExample(permissionExample);
            U.assertException(count > 0, "传入的菜单下已经有权限了, 请先将权限删除再来删除菜单");

            ManagerMenuExample menuExample = new ManagerMenuExample();
            menuExample.or().andIdIn(mids);
            menuMapper.deleteByExample(menuExample);
        }
    }


    @Override
    public List<ManagerPermission> queryPermission(String name) {
        ManagerPermissionExample permissionExample = new ManagerPermissionExample();
        if (U.isNotBlank(name)) {
            permissionExample.or().andNameLike(U.like(name));
        }
        return permissionMapper.selectByExample(permissionExample);
    }

    @Override
    public void addOrUpdatePermission(ManagerPermission permission) {
        Long pid = permission.getId();
        if (U.greater0(pid)) {
            ManagerPermission p = permissionMapper.selectByPrimaryKey(pid);
            U.assertNil(p, "没有这个权限, 无法修改");

            permissionMapper.updateByPrimaryKeySelective(permission);
        } else {
            ManagerPermissionExample permissionExample = new ManagerPermissionExample();
            permissionExample.or().andMethodEqualTo(permission.getMethod()).andUrlEqualTo(permission.getUrl());
            long count = permissionMapper.countByExample(permissionExample);
            U.assertException(count > 0, "已经有同样规则的权限, 不能再次添加");

            permission.setId(null);
            permissionMapper.insertSelective(permission);
        }
    }

    @Override
    public void deletePermission(Long permissionId) {
        permissionMapper.deleteByPrimaryKey(permissionId);
    }

    @Override
    public void deletePermissions(List<Long> pids) {
        if (A.isNotEmpty(pids)) {
            ManagerPermissionExample permissionExample = new ManagerPermissionExample();
            permissionExample.or().andIdIn(pids);
            permissionMapper.deleteByExample(permissionExample);
        }
    }
}
