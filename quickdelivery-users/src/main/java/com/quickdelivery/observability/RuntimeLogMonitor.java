package com.quickdelivery.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.quickdelivery.abstarct.dto.HourlyLogCountDTO;
import com.quickdelivery.abstarct.dto.LogEventDTO;
import com.quickdelivery.abstarct.dto.ServiceLogInsightsDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuntimeLogMonitor extends AppenderBase<ILoggingEvent> {

    private static final long RETENTION_MS = 24L * 60L * 60L * 1000L;
    private static final int MAX_EVENTS = 5000;
    private static final DateTimeFormatter HOUR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH'h'", Locale.FRANCE);

    private final Object lock = new Object();
    private final Deque<LoggedEvent> events = new ArrayDeque<>();
    private Logger rootLogger;

    @PostConstruct
    public void initialize() {
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        setContext(rootLogger.getLoggerContext());
        setName("quickdelivery-users-runtime-log-monitor");
        start();
        rootLogger.addAppender(this);
    }

    @PreDestroy
    public void shutdownAppender() {
        if (rootLogger != null) {
            rootLogger.detachAppender(this);
        }
        stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!eventObject.getLevel().isGreaterOrEqual(Level.WARN)) {
            return;
        }
        synchronized (lock) {
            prune(System.currentTimeMillis());
            events.addLast(new LoggedEvent(
                    eventObject.getTimeStamp(),
                    eventObject.getLevel().toString(),
                    categorize(eventObject),
                    abbreviate(eventObject.getFormattedMessage())
            ));
            while (events.size() > MAX_EVENTS) {
                events.removeFirst();
            }
        }
    }

    public ServiceLogInsightsDTO snapshot(String serviceName) {
        long now = System.currentTimeMillis();
        long lastHourCutoff = now - (60L * 60L * 1000L);
        synchronized (lock) {
            prune(now);
            List<LoggedEvent> currentEvents = new ArrayList<>(events);

            ServiceLogInsightsDTO insights = new ServiceLogInsightsDTO();
            insights.setServiceName(serviceName);
            insights.setWarnCountLastHour(currentEvents.stream()
                    .filter(event -> event.timestamp >= lastHourCutoff && "WARN".equals(event.level))
                    .count());
            insights.setErrorCountLastHour(currentEvents.stream()
                    .filter(event -> event.timestamp >= lastHourCutoff && "ERROR".equals(event.level))
                    .count());
            insights.setCategoryCounts(currentEvents.stream()
                    .collect(Collectors.groupingBy(event -> event.category, LinkedHashMap::new, Collectors.counting())));
            insights.setHourlyCounts(buildHourlyCounts(currentEvents, now));
            insights.setRecentEvents(currentEvents.stream()
                    .sorted(Comparator.comparingLong((LoggedEvent event) -> event.timestamp).reversed())
                    .limit(12)
                    .map(this::toDto)
                    .toList());
            return insights;
        }
    }

    private void prune(long now) {
        long cutoff = now - RETENTION_MS;
        while (!events.isEmpty() && events.peekFirst().timestamp < cutoff) {
            events.removeFirst();
        }
    }

    private List<HourlyLogCountDTO> buildHourlyCounts(List<LoggedEvent> currentEvents, long now) {
        List<HourlyLogCountDTO> buckets = new ArrayList<>();
        long currentHourStart = (now / (60L * 60L * 1000L)) * (60L * 60L * 1000L);
        for (int offset = 23; offset >= 0; offset -= 1) {
            long bucketStart = currentHourStart - (offset * 60L * 60L * 1000L);
            long bucketEnd = bucketStart + (60L * 60L * 1000L);
            long warnCount = currentEvents.stream()
                    .filter(event -> event.timestamp >= bucketStart && event.timestamp < bucketEnd && "WARN".equals(event.level))
                    .count();
            long errorCount = currentEvents.stream()
                    .filter(event -> event.timestamp >= bucketStart && event.timestamp < bucketEnd && "ERROR".equals(event.level))
                    .count();
            HourlyLogCountDTO bucket = new HourlyLogCountDTO();
            bucket.setHourLabel(HOUR_LABEL_FORMATTER.format(Instant.ofEpochMilli(bucketStart).atZone(ZoneId.systemDefault())));
            bucket.setWarnCount(warnCount);
            bucket.setErrorCount(errorCount);
            buckets.add(bucket);
        }
        return buckets;
    }

    private LogEventDTO toDto(LoggedEvent event) {
        LogEventDTO dto = new LogEventDTO();
        dto.setTimestamp(event.timestamp);
        dto.setLevel(event.level);
        dto.setCategory(event.category);
        dto.setMessage(event.message);
        return dto;
    }

    private String categorize(ILoggingEvent eventObject) {
        String base = ((eventObject.getLoggerName() == null ? "" : eventObject.getLoggerName()) + " " + eventObject.getFormattedMessage()).toLowerCase(Locale.ROOT);
        if (containsAny(base, "eureka", "discovery", "heartbeat", "registry", "unknownhost", "name resolution")) {
            return "Discovery";
        }
        if (containsAny(base, "configserver", "config server", "propertysource", "release-on-vm", "deploy", "artifact")) {
            return "Configuration";
        }
        if (containsAny(base, "sql", "jdbc", "hibernate", "datasource", "hikari")) {
            return "Database";
        }
        if (containsAny(base, "ocr", "textract")) {
            return "OCR";
        }
        if (containsAny(base, "jwt", "oauth", "keycloak", "token", "auth")) {
            return "Auth";
        }
        if (containsAny(base, "smtp", "mail")) {
            return "Mail";
        }
        if (containsAny(base, "timeout", "connection refused", "i/o error", "connectexception")) {
            return "Network";
        }
        return "Application";
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "";
        }
        String sanitized = message.replaceAll("\\s+", " ").trim();
        return sanitized.length() > 220 ? sanitized.substring(0, 217) + "..." : sanitized;
    }

    private record LoggedEvent(long timestamp, String level, String category, String message) {
    }
}
