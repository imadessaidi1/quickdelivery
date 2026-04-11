package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.parameters.NOTIFICATION_EVENT_TYPE;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class NotificationMessageLocalizer {
    private static final String BUNDLE_NAME = "notificationMessages";

    public LocalizedNotification localize(NOTIFICATION_EVENT_TYPE eventType, Locale locale, Object... arguments) {
        Locale resolvedLocale = locale == null ? Locale.FRENCH : locale;
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, resolvedLocale);
        String baseKey = "notification." + eventType.name();
        String title = format(bundle.getString(baseKey + ".title"), arguments, resolvedLocale);
        String body = format(bundle.getString(baseKey + ".body"), arguments, resolvedLocale);
        return new LocalizedNotification(title, body);
    }

    private String format(String pattern, Object[] arguments, Locale locale) {
        return new MessageFormat(pattern, locale).format(arguments == null ? new Object[]{} : arguments);
    }

    public record LocalizedNotification(String title, String body) {
    }
}
