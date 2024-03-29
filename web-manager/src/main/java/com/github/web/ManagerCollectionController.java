package com.github.web;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.annotation.NotNeedPermission;
import com.github.common.util.A;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiIgnore;
import com.github.liuanxin.api.annotation.ApiMethod;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.*;

@ApiIgnore
@RestController
public class ManagerCollectionController {

    private final RequestMappingHandlerMapping mapping;
    public ManagerCollectionController(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    /** 把当下系统里的接口信息整理成 menu 和 permission 表的 insert 语句 */
    @GetMapping("/collect-menu-permission")
    public boolean version() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        Map<String, Set<String>> multiMap = new HashMap<>();
        String sp = "~!~";
        String classSuffix = "Controller";
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo requestMapping = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            NotNeedLogin login = getAnnotation(handlerMethod, NotNeedLogin.class);
            // 如果有标 不需要登录 则表示这个请求不需要收集
            if (U.isNotNull(login) && login.value()) {
                continue;
            }
            // 如果有标 不需要权限 则表示这个请求不需要收集
            NotNeedPermission permission = getAnnotation(handlerMethod, NotNeedPermission.class);
            if (U.isNotNull(permission) && login.value()) {
                continue;
            }

            if (U.isNotNull(requestMapping) && wasJsonApi(handlerMethod)) {
                String className = handlerMethod.getBeanType().getSimpleName();
                if (className.endsWith(classSuffix)) {
                    className = className.substring(0, className.indexOf(classSuffix));
                }
                ApiGroup apiGroup = getAnnotation(handlerMethod, ApiGroup.class);
                String menu;
                if (U.isNull(apiGroup)) {
                    menu = className;
                } else {
                    String[] t = apiGroup.value()[0].split("-");
                    menu = t.length > 1 ? t[1] : t[0];
                }

                ApiMethod apiMethod = handlerMethod.getMethodAnnotation(ApiMethod.class);
                String permissionName = U.isNull(apiMethod) ? U.EMPTY : apiMethod.value();
                // 类名用作 front
                String key = String.join(sp, Arrays.asList(menu, className));

                String method = A.toStr(requestMapping.getMethodsCondition().getMethods());
                PatternsRequestCondition prc = requestMapping.getPatternsCondition();
                String url = U.isNull(prc) ? U.EMPTY : String.join(",", prc.getPatterns());
                String value = String.join(sp, Arrays.asList(permissionName, method, url));

                multiMap.computeIfAbsent(key, (k1) -> new LinkedHashSet<>()).add(value);
            }
        }
        int i = 1;
        for (Map.Entry<String, Set<String>> entry : multiMap.entrySet()) {
            String[] menuArr = entry.getKey().split(sp);
            System.out.printf(
                    "REPLACE INTO `t_manager_menu`(`id`, `name`, `front`) VALUES(%s,'%s','%s')%n",
                    i, menuArr[0], menuArr[1]
            );

            int j = 1;
            for (String permission : entry.getValue()) {
                String[] arr = permission.split(sp);
                System.out.printf(
                        "REPLACE INTO `t_manager_permission`(`id`, `mid`, `name`, `method`, `url`)" +
                                " VALUES(%s, %s, '%s', '%s', '%s')%n",
                        j, i, arr[0], arr[1], arr[2]
                );
                j++;
            }
            System.out.println("\n");
            i++;
        }
        return true;
    }

    private static boolean wasJsonApi(HandlerMethod handlerMethod) {
        if (U.isNotNull(getAnnotation(handlerMethod, ResponseBody.class))) {
            return true;
        } else {
            return U.isNotNull(getAnnotationByClass(handlerMethod, RestController.class));
        }
    }
    private static <T extends Annotation> T getAnnotationByClass(HandlerMethod handlerMethod, Class<T> clazz) {
        return AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), clazz);
    }
    private static <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> clazz) {
        T annotation = handlerMethod.getMethodAnnotation(clazz);
        if (U.isNull(annotation)) {
            annotation = getAnnotationByClass(handlerMethod, clazz);
        }
        return annotation;
    }
}
