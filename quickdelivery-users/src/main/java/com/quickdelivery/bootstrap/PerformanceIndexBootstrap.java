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
public class PerformanceIndexBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceIndexBootstrap.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public ApplicationRunner performanceIndexInitializer(JdbcTemplate jdbcTemplate) {
        return args -> {
            ensureIndex(jdbcTemplate, "user", "idx_user_active_account",
                    "CREATE INDEX idx_user_active_account ON `user` (active_account)");
            ensureIndex(jdbcTemplate, "user", "idx_user_email_address",
                    "CREATE INDEX idx_user_email_address ON `user` (email_address)");
            ensureIndex(jdbcTemplate, "document", "idx_document_user_type",
                    "CREATE INDEX idx_document_user_type ON document (user_id, type)");
            ensureIndex(jdbcTemplate, "document", "idx_document_vehicle_type",
                    "CREATE INDEX idx_document_vehicle_type ON document (vehicle_id, type)");
            ensureIndex(jdbcTemplate, "document", "idx_document_package_type",
                    "CREATE INDEX idx_document_package_type ON document (package_id, type)");
            ensureIndex(jdbcTemplate, "document", "idx_document_status",
                    "CREATE INDEX idx_document_status ON document (document_status)");
            ensureIndex(jdbcTemplate, "document", "idx_document_ocr_status",
                    "CREATE INDEX idx_document_ocr_status ON document (ocr_status)");
            ensureIndex(jdbcTemplate, "document", "idx_document_match_status",
                    "CREATE INDEX idx_document_match_status ON document (match_status)");
        };
    }

    private void ensureIndex(JdbcTemplate jdbcTemplate, String tableName, String indexName, String sql) {
        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (indexCount != null && indexCount > 0) {
            return;
        }

        logger.info("Adding missing index {} on {}", indexName, tableName);
        jdbcTemplate.execute(sql);
    }
}
