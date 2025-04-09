package com.quickdelivery.abstarct.helpers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class MailHelper {
    private static JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername("quickdelivery085@gmail.com");
        mailSender.setPassword("tkfv dmdw nomn beba");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }



    private static SpringTemplateEngine thymeleafTemplateEngine(ITemplateResolver templateResolver, ResourceBundleMessageSource messageSource) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setTemplateEngineMessageSource(messageSource);
        return templateEngine;
    }

    private static void sendHtmlMessage(String to, String subject, String htmlBody, String pathToAttachment) throws MessagingException {
        MimeMessage message = getJavaMailSender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        if(pathToAttachment != null) {
            FileSystemResource file
                    = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment(file.getFilename(), file);
        }
        getJavaMailSender().send(message);
    }

    public static void sendSimpleMessage(
            String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        getJavaMailSender().send(message);
    }

    public static void sendMessageWithAttachment(
            String to, String subject, String text, String pathToAttachment) {
        MimeMessage message = getJavaMailSender().createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            FileSystemResource file
                    = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment(file.getFilename(), file);
            getJavaMailSender().send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessageUsingThymeleafTemplate(ResourceBundleMessageSource messageSource, ITemplateResolver templateResolver,
            String to, String subject, Map<String, Object> templateModel, Locale locale, String template, String pathToAttachment)
            throws MessagingException {

        Context thymeleafContext = new Context(locale);
        thymeleafContext.setVariables(templateModel);
        String htmlBody = thymeleafTemplateEngine(templateResolver,messageSource).process(template, thymeleafContext);

        sendHtmlMessage(to, subject, htmlBody, pathToAttachment);
    }
}