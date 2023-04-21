//package com.github.global.service;
//
//import com.github.common.util.LogUtil;
//import com.github.common.util.U;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.mail.MailException;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//import org.springframework.mail.javamail.MimeMessageHelper;
//
//import jakarta.mail.Message;
//import jakarta.mail.internet.InternetAddress;
//import java.io.File;
//import java.util.List;
//
///**
// * @see org.springframework.boot.autoconfigure.mail.MailProperties
// * @see org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration
// */
//@RequiredArgsConstructor
//@Configuration
//@ConditionalOnClass({ Message.class, JavaMailSender.class })
//public class MailSender {
//
//    private final JavaMailSender mailSender;
//
//    public void send(String to, String subject, String htmlContent, List<File> attachFileList) {
//        send(((JavaMailSenderImpl) mailSender).getUsername(), to, subject, htmlContent, attachFileList);
//    }
//
//    public void send(String from, String to, String subject, String htmlContent, List<File> attachFileList) {
//        U.assertBlank(from, "Need from email address");
//        try {
//            mailSender.send(mimeMessage -> {
//                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
//                mimeMessage.setFrom(new InternetAddress(from));
//                mimeMessage.setSubject(subject);
//
//                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
//                helper.setText(htmlContent, true);
//                for (File file : attachFileList) {
//                    helper.addAttachment(file.getName(), new FileSystemResource(file));
//                }
//            });
//        } catch (MailException ex) {
//            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
//                LogUtil.ROOT_LOG.error("{} -> {} (title: {}, content: {}, attach: {}) exception",
//                        from, to, subject, htmlContent, attachFileList, ex);
//            }
//        }
//    }
//}
