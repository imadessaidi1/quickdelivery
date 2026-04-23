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
        mailSender.setHost(resolveMailSetting("quickdelivery.mail.host", "QUICKDELIVERY_MAIL_HOST", "smtp.ionos.fr"));
        mailSender.setPort(Integer.parseInt(resolveMailSetting("quickdelivery.mail.port", "QUICKDELIVERY_MAIL_PORT", "587")));
        String username = resolveRequiredMailSecret("quickdelivery.mail.username", "QUICKDELIVERY_MAIL_USERNAME");
        mailSender.setUsername(username);
        mailSender.setPassword(resolveRequiredMailSecret("quickdelivery.mail.password", "QUICKDELIVERY_MAIL_PASSWORD"));
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", resolveMailSetting("quickdelivery.mail.smtp.auth", "QUICKDELIVERY_MAIL_SMTP_AUTH", "true"));
        props.put("mail.smtp.starttls.enable", resolveMailSetting("quickdelivery.mail.smtp.starttls.enable", "QUICKDELIVERY_MAIL_SMTP_STARTTLS_ENABLE", "true"));
        props.put("mail.smtp.from", resolveMailFromAddress(username));
        props.put("mail.debug", resolveMailSetting("quickdelivery.mail.debug", "QUICKDELIVERY_MAIL_DEBUG", "false"));
        props.put("mail.mime.charset", "UTF-8");
        props.put("mail.mime.allowutf8", "true");

        return mailSender;
    }

    private static String resolveMailSetting(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String resolveMailFromAddress(String fallbackUsername) {
        String from = resolveMailSetting("quickdelivery.mail.from", "QUICKDELIVERY_MAIL_FROM", fallbackUsername);
        if (from == null || from.isBlank()) {
            return fallbackUsername;
        }
        return from.trim();
    }

    private static String resolveRequiredMailSecret(String propertyName, String envName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing mail configuration: " + propertyName + " / " + envName);
        }
        if ("quickdelivery.mail.password".equals(propertyName)) {
            return value.replaceAll("\\s+", "");
        }
        return value.trim();
    }

    private static SpringTemplateEngine thymeleafTemplateEngine(ITemplateResolver templateResolver, ResourceBundleMessageSource messageSource) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        if (messageSource != null) {
            messageSource.setDefaultEncoding("UTF-8");
            templateEngine.setTemplateEngineMessageSource(messageSource);
        }
        return templateEngine;
    }

    private static void sendHtmlMessage(String to, String subject, String htmlBody, String pathToAttachment) throws MessagingException {
        JavaMailSender mailSender = getJavaMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(resolveMailFromAddress(resolveRequiredMailSecret("quickdelivery.mail.username", "QUICKDELIVERY_MAIL_USERNAME")));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        if(pathToAttachment != null) {
            FileSystemResource file
                    = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment(file.getFilename(), file);
        }
        mailSender.send(message);
    }

    public static void sendSimpleMessage(
            String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(resolveMailFromAddress(resolveRequiredMailSecret("quickdelivery.mail.username", "QUICKDELIVERY_MAIL_USERNAME")));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        getJavaMailSender().send(message);
    }

    public static void sendMessageWithAttachment(
            String to, String subject, String text, String pathToAttachment) {
        JavaMailSender mailSender = getJavaMailSender();
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveMailFromAddress(resolveRequiredMailSecret("quickdelivery.mail.username", "QUICKDELIVERY_MAIL_USERNAME")));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            FileSystemResource file
                    = new FileSystemResource(new File(pathToAttachment));
            helper.addAttachment(file.getFilename(), file);
            mailSender.send(message);
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
