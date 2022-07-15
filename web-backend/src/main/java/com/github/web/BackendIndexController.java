package com.github.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.common.json.JsonResult;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.SecurityCodeUtil;
import com.github.common.util.U;
import com.github.global.service.ValidatorService;
import com.github.liuanxin.api.annotation.ApiIgnore;
import com.github.util.BackendSessionUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApiIgnore
@Controller
public class BackendIndexController {

    private final ValidatorService validatorService;
    private final RequestMappingHandlerMapping mapping;
    public BackendIndexController(ValidatorService validatorService,
                                  @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.validatorService = validatorService;
        this.mapping = mapping;
    }

    @GetMapping("/")
    @ResponseBody
    public String index() {
        return "backend-api";
    }

    @GetMapping("/code")
    public void code(HttpServletResponse response, String width, String height,
                     String count, String style, String rgb) throws IOException {
        SecurityCodeUtil.Code code = SecurityCodeUtil.generateCode(count, style, width, height, rgb);

        // 往 session 里面丢值
        BackendSessionUtil.putImageCode(code.getContent());

        // 向页面渲染图像
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/png");
        javax.imageio.ImageIO.write(code.getImage(), "png", response.getOutputStream());
    }

    @GetMapping("/collect")
    @ResponseBody
    public String collect() {
        List<Map<String, String>> urlList = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();
            urlList.add(Map.of(
                    "url", U.isNull(info.getPatternsCondition()) ? U.EMPTY : A.toStr(info.getPatternsCondition().getPatterns()),
                    "method", A.toStr(info.getMethodsCondition().getMethods()),
                    "class", method.getBeanType().getName() + "#" + method.getMethod().getName()
            ));
        }
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("接口信息: ({})", JsonUtil.toJson(urlList));
        }
        return "collect";
    }

    @ResponseBody
    @PostMapping("/test")
    public JsonResult<String> test(@RequestBody Test test) {
        validatorService.handleValidate(test);
        return JsonResult.success("test");
    }

    @Data
    public static class Test {
        @JsonProperty("abcName")
        @Size(min = 2, max = 5, message = "{404}")
        private String name;

        @JsonProperty("showGender")
        @NotNull(message = "{400}")
        private Integer gender;

        @Valid
        @JsonProperty("ml")
        private Test1 model;
    }
    @Data
    public static class Test1 {
        @Size(min = 2, max = 5, message = "xxx")
        private String id;

        @NotNull(message = "{403}")
        @JsonProperty("types")
        private Integer type;
    }
}
