package com.github.manager.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.common.collection.MapMultiValue;
import com.github.common.collection.MultiUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import lombok.Data;

import java.util.*;

/** 菜单, 需要跟前端对应, 前端每增加一个菜单就需要添加一条记录, 与角色是 多对多 的关系 --> t_manager_menu */
@Data
@TableName("t_manager_menu")
public class ManagerMenu {

    /** 根 id */
    private static final long ROOT_ID = 0L;


    private Long id;

    /** 父菜单, 0 则表示是根菜单 --> pid */
    private Long pid;

    /** 菜单说明 --> name */
    private String name;

    /** 前端对应的值(如 path 或 name) --> front */
    private String front;


    // 下面的字段不与数据库关联, 只做为数据载体进行传输

    /** 子菜单 */
    @TableField(exist = false)
    private List<ManagerMenu> children;

    /** 菜单下的权限 */
    @TableField(exist = false)
    private List<ManagerPermission> permissionList;

    private static void handle(ManagerMenu menu, Map<String, List<ManagerMenu>> menuMap, int depth) {
        List<ManagerMenu> menus = menuMap.get(U.toStr(menu.getId()));
        if (A.isNotEmpty(menus) && depth < U.MAX_DEPTH) {
            for (ManagerMenu m : menus) {
                handle(m, menuMap, depth + 1);
            }
            menu.setChildren(new ArrayList<>(menus));
        }
    }
    /** 将多条菜单整理成有父子关系的菜单, 且每条菜单中填充它下面的权限, 以 RoleId 为 key, List<Menu> 为 value 的 Map 形式返回 */
    public static Map<String, List<ManagerMenu>> handleRelation(List<ManagerMenu> menus,
                                                                List<ManagerPermission> permissions,
                                                                Map<String, Collection<Long>> roleIdMenuIdMap) {
        if (A.isEmpty(roleIdMenuIdMap) || A.isNotEmpty(menus)) {
            return Collections.emptyMap();
        }

        Map<Long, List<ManagerPermission>> mpMap = A.listToMapList(permissions, ManagerPermission::getMenuId);

        List<ManagerMenu> menuList = new ArrayList<>();
        MapMultiValue<String, ManagerMenu, List<ManagerMenu>> childMap = MultiUtil.createMapList();
        for (ManagerMenu menu : menus) {
            // 将权限写进菜单
            List<ManagerPermission> mps = mpMap.get(menu.getId());
            if (A.isNotEmpty(mps)) {
                menu.setPermissionList(mps);
            }

            if (menu.getPid() == ROOT_ID) {
                menuList.add(menu);
            } else {
                childMap.put(U.toStr(menu.getPid()), menu);
            }
        }
        Map<String, List<ManagerMenu>> relationMap = childMap.asMap();
        for (ManagerMenu menu : menuList) {
            handle(menu, relationMap, 0);
        }

        Map<String, ManagerMenu> tmpMap = new HashMap<>();
        for (ManagerMenu menu : menuList) {
            tmpMap.put(U.toStr(menu.getId()), menu);
        }

        Map<String, List<ManagerMenu>> returnMap = new HashMap<>();
        if (A.isNotEmpty(tmpMap)) {
            for (Map.Entry<String, Collection<Long>> entry : roleIdMenuIdMap.entrySet()) {
                List<ManagerMenu> managerMenus = new ArrayList<>();
                for (Long mid : entry.getValue()) {
                    managerMenus.add(tmpMap.get(U.toStr(mid)));
                }
                returnMap.put(entry.getKey(), managerMenus);
            }
        }
        return returnMap;
    }

    /** 将有层级关系的菜单平级返回 */
    static List<ManagerMenu> handleAllMenu(List<ManagerMenu> menus) {
        if (A.isEmpty(menus)) {
            return Collections.emptyList();
        } else {
            List<ManagerMenu> returnList = new ArrayList<>();
            allMenuUseDepth(returnList, menus);
            // allMenuUseBreadth(returnList, menus);
            return returnList;
        }
    }
    /**
     * <pre>
     * 使用深度优先(Depth-First)
     *
     * 比如:
     * [
     *   { "id": 1, "children": [ { "id": 11, "children": [ { "id": 111 } ] }, { "id": 12 } ] },
     *   { "id": 2, "children": [ { "id": 21 }, { "id": 22, "children": [ { "id": 221 } ] } ] }
     * ]
     *
     * 返回:
     * [ { "id": 1 }, { "id": 11 }, { "id": 111 }, { "id": 12 }, { "id": 2 }, { "id": 21 }, { "id": 22 }, { "id": 221 } ]
     * </pre>
     */
    private static void allMenuUseDepth(List<ManagerMenu> returnList, List<ManagerMenu> menus) {
        allMenuUseDepthWithDepth(returnList, menus, 0);
    }
    private static void allMenuUseDepthWithDepth(List<ManagerMenu> returnList, List<ManagerMenu> menus, int depth) {
        if (A.isNotEmpty(menus)) {
            for (ManagerMenu menu : menus) {
                returnList.add(menu);

                List<ManagerMenu> children = menu.getChildren();
                if (A.isNotEmpty(children) && depth <= U.MAX_DEPTH) {
                    allMenuUseDepthWithDepth(returnList, children, depth + 1);
                }
            }
        }
    }
    /**
     * <pre>
     * 使用广度优先(Breadth-First)
     *
     * 比如:
     * [
     *   { "id": 1, "children": [ { "id": 11, "children": [ { "id": 111 } ] }, { "id": 12 } ] },
     *   { "id": 2, "children": [ { "id": 21 }, { "id": 22, "children": [ { "id": 221 } ] } ] }
     * ]
     *
     * 返回:
     * [ { "id": 1 }, { "id": 2 }, { "id": 11 }, { "id": 12 }, { "id": 111 }, { "id": 21 }, { "id": 22 }, { "id": 221 } ]
     * </pre>
     */
    private static void allMenuUseBreadth(List<ManagerMenu> returnList, List<ManagerMenu> menus) {
        allMenuUseBreadthWithDepth(returnList, menus, 0);
    }
    private static void allMenuUseBreadthWithDepth(List<ManagerMenu> returnList, List<ManagerMenu> menus, int depth) {
        if (A.isNotEmpty(menus)) {
            returnList.addAll(menus);

            for (ManagerMenu menu : menus) {
                List<ManagerMenu> children = menu.getChildren();
                if (A.isNotEmpty(children) && depth <= U.MAX_DEPTH) {
                    allMenuUseBreadthWithDepth(returnList, children, depth + 1);
                }
            }
        }
    }
}
