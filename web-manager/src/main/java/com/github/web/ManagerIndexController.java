package com.github.web;

import com.github.common.util.SecurityCodeUtil;
import com.github.liuanxin.api.annotation.ApiIgnore;
import com.github.util.ManagerSessionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ApiIgnore
@Controller
public class ManagerIndexController {

    @ResponseBody
    @GetMapping("/")
    public String index() {
        return "manager-api";
    }

    @GetMapping("/code")
    public void code(HttpServletResponse response, String width, String height,
                     String count, String style, String background, String color) throws IOException {
        SecurityCodeUtil.Code code = SecurityCodeUtil.generateCode(count, style, width, height, background, color);

        // 往 session 里面丢值
        ManagerSessionUtil.putImageCode(code.content());

        // 向页面渲染图像
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/png");
        javax.imageio.ImageIO.write(code.image(), "png", response.getOutputStream());
    }
}
