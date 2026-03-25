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
            ensureIndex(jdbcTemplate, "package", "idx_package_status",
                    "CREATE INDEX idx_package_status ON `package` (status)");
            ensureIndex(jdbcTemplate, "package", "idx_package_sender_id",
                    "CREATE INDEX idx_package_sender_id ON `package` (sender_id)");
            ensureIndex(jdbcTemplate, "package", "idx_package_reference",
                    "CREATE INDEX idx_package_reference ON `package` (reference)");
            ensureIndex(jdbcTemplate, "package", "idx_package_guest_access_token",
                    "CREATE INDEX idx_package_guest_access_token ON `package` (guest_access_token)");
            ensureIndex(jdbcTemplate, "package_reservation", "idx_package_reservation_delivery_status",
                    "CREATE INDEX idx_package_reservation_delivery_status ON package_reservation (deliveryPerson_id, status)");
            ensureIndex(jdbcTemplate, "package_reservation", "idx_package_reservation_package_status",
                    "CREATE INDEX idx_package_reservation_package_status ON package_reservation (package_id, status)");
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
