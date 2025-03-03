package com.github.global.service;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;

/**
 * @see org.springframework.boot.autoconfigure.mail.MailProperties
 * @see org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ Message.class, JavaMailSender.class })
public class MailSender {

    private final JavaMailSender mailSender;

    public void sendText(String to, String subject, String textContent) {
        // 从配置文件中获取发送邮件地址
        sendText(((JavaMailSenderImpl) mailSender).getUsername(), to, subject, textContent);
    }

    public void sendText(String from, String to, String subject, String textContent) {
        U.assertBlank(from, "Need from email address");
        U.assertBlank(to, "Need to email address");
        U.assertBlank(subject, "Need email subject");
        U.assertBlank(textContent, "Need email content");

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setFrom(from);
            mailMessage.setSubject(subject);
            mailMessage.setText(textContent);
            mailSender.send(mailMessage);
        } catch (MailException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{} -> {} (title: {}, content: {}) exception",
                        from, to, subject, U.toStr(textContent, 500, 100), e);
            }
        }
    }

    public void sendHtml(String to, String subject, String htmlContent, List<File> attachFileList) {
        // 从配置文件中获取发送邮件地址
        sendHtml(((JavaMailSenderImpl) mailSender).getUsername(), to, subject, htmlContent, attachFileList);
    }

    public void sendHtml(String from, String to, String subject, String htmlContent, List<File> attachFileList) {
        U.assertBlank(from, "Need from email address");
        U.assertBlank(to, "Need to email address");
        U.assertBlank(subject, "Need email subject");
        U.assertBlank(htmlContent, "Need email content");

        try {
            mailSender.send(mimeMessage -> {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress(from));
                mimeMessage.setSubject(subject);

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setText(htmlContent, true);
                for (File file : attachFileList) {
                    helper.addAttachment(file.getName(), new FileSystemResource(file));
                }
            });
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{} -> {} (title: {}, content: {}, attach: {}) exception",
                        from, to, subject, U.toStr(htmlContent, 500, 100), attachFileList, e);
            }
        }
    }
}
