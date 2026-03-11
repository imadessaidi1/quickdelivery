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
        mailSender.setHost(resolveMailSetting("quickdelivery.mail.host", "QUICKDELIVERY_MAIL_HOST", "smtp.gmail.com"));
        mailSender.setPort(Integer.parseInt(resolveMailSetting("quickdelivery.mail.port", "QUICKDELIVERY_MAIL_PORT", "587")));
        mailSender.setUsername(resolveRequiredMailSecret("quickdelivery.mail.username", "QUICKDELIVERY_MAIL_USERNAME"));
        mailSender.setPassword(resolveRequiredMailSecret("quickdelivery.mail.password", "QUICKDELIVERY_MAIL_PASSWORD"));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", resolveMailSetting("quickdelivery.mail.smtp.auth", "QUICKDELIVERY_MAIL_SMTP_AUTH", "true"));
        props.put("mail.smtp.starttls.enable", resolveMailSetting("quickdelivery.mail.smtp.starttls.enable", "QUICKDELIVERY_MAIL_SMTP_STARTTLS_ENABLE", "true"));
        props.put("mail.debug", resolveMailSetting("quickdelivery.mail.debug", "QUICKDELIVERY_MAIL_DEBUG", "false"));

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
        return value;
    }

    private static String resolveRequiredMailSecret(String propertyName, String envName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing mail configuration: " + propertyName + " / " + envName);
        }
        return value;
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
