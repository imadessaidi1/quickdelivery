package com.quickdelivery.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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
            ensureIndex(jdbcTemplate, "package", "idx_package_status_creation",
                    "CREATE INDEX idx_package_status_creation ON `package` (status, creation_date DESC, id DESC)");
            ensureIndex(jdbcTemplate, "address", "idx_address_type_latitude_longitude",
                    "CREATE INDEX idx_address_type_latitude_longitude ON address (type, latitude, longitude)");
            ensureIndex(jdbcTemplate, "address", "idx_address_package_id_type",
                    "CREATE INDEX idx_address_package_id_type ON address (package_id, type)");
            ensureGeoPointColumn(jdbcTemplate);
            ensurePackageReservationDeliveryStatusIndex(jdbcTemplate);
            ensureIndex(jdbcTemplate, "package_reservation", "idx_package_reservation_package_status",
                    "CREATE INDEX idx_package_reservation_package_status ON package_reservation (package_id, status)");
        };
    }

    private void ensureGeoPointColumn(JdbcTemplate jdbcTemplate) {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'address' AND COLUMN_NAME = 'geo_point'",
                String.class
        );
        if (columns.isEmpty()) {
            return;
        }
        ensureIndex(jdbcTemplate, "address", "idx_address_geo_point",
                "CREATE SPATIAL INDEX idx_address_geo_point ON address (geo_point)");
    }

    private void ensurePackageReservationDeliveryStatusIndex(JdbcTemplate jdbcTemplate) {
        String deliveryPersonColumn = resolveExistingColumn(
                jdbcTemplate,
                "package_reservation",
                "deliveryPerson_id",
                "delivery_person_id"
        );

        if (deliveryPersonColumn == null) {
            logger.warn("Skipping index idx_package_reservation_delivery_status: no delivery person column found on package_reservation");
            return;
        }

        ensureIndex(
                jdbcTemplate,
                "package_reservation",
                "idx_package_reservation_delivery_status",
                "CREATE INDEX idx_package_reservation_delivery_status ON package_reservation (" + deliveryPersonColumn + ", status)"
        );
    }

    private String resolveExistingColumn(JdbcTemplate jdbcTemplate, String tableName, String... candidates) {
        List<String> existingColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class,
                tableName
        );

        for (String candidate : candidates) {
            if (existingColumns.contains(candidate)) {
                return candidate;
            }
        }
        return null;
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
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception exception) {
            logger.warn("Skipping index {} on {} because creation failed: {}", indexName, tableName, exception.getMessage());
        }
    }
}
