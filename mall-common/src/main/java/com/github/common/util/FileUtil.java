package com.github.common.util;

import com.github.common.date.Dates;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileUtil {

    /**
     * 保存单个文件到指定的位置, 并将此文件的 url 地址返回
     * 比如 file 是 abc.jpg, directory 地址是 /mnt/data, url 是 image.xxx.com, addYmd 是 true
     * 文件将会保存在 /mnt/data/YYYY/MM/DD/uuid.jpg, 返回 //image.xxx.com/YYYY/MM/DD/uuid.jpg
     */
    public static String save(MultipartFile file, String directoryPrefix, String urlPrefix, boolean addYmd) {
        directoryPrefix = directoryPrefix.trim();
        // 保存目录以 / 开头, 结尾不带 /
        directoryPrefix = U.addPrefix(directoryPrefix);
        if (directoryPrefix.endsWith("/")) {
            directoryPrefix = directoryPrefix.substring(0, directoryPrefix.length() - 1);
        }

        urlPrefix = urlPrefix.trim();
        // 访问地址前缀以 // 开头, 结尾不带 /
        if (!urlPrefix.startsWith("http://") && !urlPrefix.startsWith("https://") && !urlPrefix.startsWith("//")) {
            urlPrefix = "//" + urlPrefix;
        } else {
            urlPrefix = urlPrefix.replaceFirst("http(s?)://", "//");
        }
        if (urlPrefix.endsWith("/")) {
            urlPrefix = urlPrefix.substring(0, urlPrefix.length() - 1);
        }

        // 保存及访问地址中拼上 /年/月/日/ 按日来保存文件, 前后都要有 /
        String middlePath = addYmd ? ("/" + Dates.formatUsaDate(LocalDate.now()) + "/") : "/";
        // 目录不存在就生成
        File directory = new File(directoryPrefix + middlePath);
        if (!directory.exists()) {
            // noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
        // 重命名
        String newName = U.renameFile(file.getOriginalFilename());

        try {
            file.transferTo(new File(directory, newName));
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("upload file exception", e);
            }
            U.serviceException("文件上传时异常");
        }
        return urlPrefix + middlePath + newName;
    }

    /** 保存多个文件到指定的位置, 并将 { 原文件名1: url 地址 } 返回 */
    public static Map<String, String> save(List<MultipartFile> fileList, String directoryPrefix, String urlPrefix) {
        Map<String, String> nameUrlMap = new HashMap<>();
        for (MultipartFile file : fileList) {
            String url = save(file, directoryPrefix, urlPrefix, false);
            nameUrlMap.put(file.getOriginalFilename(), url);
        }
        return nameUrlMap;
    }
}
