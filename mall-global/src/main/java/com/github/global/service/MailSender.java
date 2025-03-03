package com.github.global.service;

import com.github.common.util.A;
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

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @see org.springframework.boot.autoconfigure.mail.MailProperties
 * @see org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ JavaMailSender.class })
public class MailSender {

    private final JavaMailSender mailSender;

    public void sendText(String to, String subject, String content) {
        sendText(Collections.singletonList(to), subject, content);
    }

    public void sendText(List<String> toList, String subject, String content) {
        sendText(((JavaMailSenderImpl) mailSender).getUsername(), toList, subject, content);
    }

    public void sendText(String from, List<String> toList, String subject, String content) {
        U.assertBlank(from, "Need from email address");
        U.assertEmpty(toList, "Need to email address");
        U.assertBlank(subject, "Need email subject");
        U.assertBlank(content, "Need email content");

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toList.toArray(String[]::new));
            mailMessage.setFrom(from);
            mailMessage.setSubject(subject);
            mailMessage.setText(content);
            mailSender.send(mailMessage);
        } catch (MailException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{} -> {} (title: {}, content: {}) exception",
                        from, toList, subject, U.toStr(content, 500, 100), e);
            }
        }
    }


    public void sendHtml(String to, String subject, String content, List<File> attachList) {
        sendHtml(Collections.singletonList(to), subject, content, attachList);
    }

    public void sendHtml(List<String> toList, String subject, String content, List<File> attachList) {
        sendHtml(((JavaMailSenderImpl) mailSender).getUsername(), toList, subject, content, attachList);
    }

    public void sendHtml(String from, List<String> toList, String subject, String content, List<File> attachList) {
        U.assertBlank(from, "Need from email address");
        U.assertEmpty(toList, "Need to email address");
        U.assertBlank(subject, "Need email subject");
        U.assertBlank(content, "Need email content");

        try {
            List<InternetAddress> addressList = new ArrayList<>();
            for (String to : toList) {
                if (U.isNotBlank(to)) {
                    addressList.add(new InternetAddress(to));
                }
            }
            mailSender.send(mimeMessage -> {
                mimeMessage.setRecipients(Message.RecipientType.TO, addressList.toArray(Address[]::new));
                mimeMessage.setFrom(new InternetAddress(from));
                mimeMessage.setSubject(subject);

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setText(content, true);
                if (A.isNotEmpty(attachList)) {
                    for (File file : attachList) {
                        helper.addAttachment(file.getName(), new FileSystemResource(file));
                    }
                }
            });
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("{} -> {} (title: {}, content: {}, attach: {}) exception",
                        from, toList, subject, U.toStr(content, 500, 100), attachList, e);
            }
        }
    }
}
