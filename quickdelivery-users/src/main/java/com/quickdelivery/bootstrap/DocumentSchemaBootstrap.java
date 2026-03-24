package com.quickdelivery.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DocumentSchemaBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSchemaBootstrap.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner documentSchemaInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            ensureColumn(jdbcTemplate, "match_status", "ALTER TABLE document ADD COLUMN match_status VARCHAR(255)");
            ensureColumn(jdbcTemplate, "match_score", "ALTER TABLE document ADD COLUMN match_score DOUBLE");
            ensureColumn(jdbcTemplate, "match_details", "ALTER TABLE document ADD COLUMN match_details LONGTEXT");
        };
    }

    private void ensureColumn(JdbcTemplate jdbcTemplate, String columnName, String sql) {
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'document' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (columnCount != null && columnCount > 0) {
            return;
        }

        logger.info("Adding missing document column {}", columnName);
        jdbcTemplate.execute(sql);
    }
}
