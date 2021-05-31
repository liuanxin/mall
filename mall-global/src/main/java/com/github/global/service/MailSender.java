package com.github.global.service;

import com.github.common.util.U;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ Message.class, JavaMailSenderImpl.class })
public class MailSender {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String htmlContent, List<File> attachFileList) {
        send(((JavaMailSenderImpl) mailSender).getUsername(), to, subject, htmlContent, attachFileList);
    }

    public void send(String from, String to, String subject, String htmlContent, List<File> attachFileList) {
        U.assertNil(from, "Need from email address");
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
        } catch (MailException ex) {
            log.error(String.format("%s -> %s (title: %s, content: %s, attach: %s) exception",
                    from, to, subject, htmlContent, attachFileList), ex);
        }
    }
}
