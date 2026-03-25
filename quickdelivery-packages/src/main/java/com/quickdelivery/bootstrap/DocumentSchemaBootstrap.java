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
            ensureColumn(jdbcTemplate, "ocr_document_type", "ALTER TABLE document ADD COLUMN ocr_document_type VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_detected_document_type", "ALTER TABLE document ADD COLUMN ocr_detected_document_type VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_type_consistent", "ALTER TABLE document ADD COLUMN ocr_type_consistent BIT");
            ensureColumn(jdbcTemplate, "ocr_last_name", "ALTER TABLE document ADD COLUMN ocr_last_name VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_first_name", "ALTER TABLE document ADD COLUMN ocr_first_name VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_birth_date", "ALTER TABLE document ADD COLUMN ocr_birth_date VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_expiry_date", "ALTER TABLE document ADD COLUMN ocr_expiry_date VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_registration_number", "ALTER TABLE document ADD COLUMN ocr_registration_number VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_brand", "ALTER TABLE document ADD COLUMN ocr_brand VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_model", "ALTER TABLE document ADD COLUMN ocr_model VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_energy_type", "ALTER TABLE document ADD COLUMN ocr_energy_type VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_holder_name", "ALTER TABLE document ADD COLUMN ocr_holder_name VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_company_name", "ALTER TABLE document ADD COLUMN ocr_company_name VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_siren", "ALTER TABLE document ADD COLUMN ocr_siren VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_insurance_kind", "ALTER TABLE document ADD COLUMN ocr_insurance_kind VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_iban", "ALTER TABLE document ADD COLUMN ocr_iban VARCHAR(255)");
            ensureColumn(jdbcTemplate, "ocr_bic", "ALTER TABLE document ADD COLUMN ocr_bic VARCHAR(255)");
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
