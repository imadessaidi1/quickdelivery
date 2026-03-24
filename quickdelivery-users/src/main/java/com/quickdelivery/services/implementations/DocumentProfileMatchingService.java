package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.entities.Vehicle;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class DocumentProfileMatchingService {
    private final ObjectMapper objectMapper;

    public DocumentProfileMatchingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MatchingResult match(Document document, Map<String, Object> structuredData) {
        if (document == null || structuredData == null) {
            return new MatchingResult(DOCUMENT_MATCH_STATUS.UNAVAILABLE, null, Map.of("reason", "missing_document_or_ocr_data"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> extractedFields = structuredData.get("fields") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        DOCUMENT_TYPE documentType = document.getType();
        User user = document.getUser();
        Vehicle vehicle = user == null ? null : user.getVehicles().stream().findFirst().orElse(null);

        return switch (documentType) {
            case DRIVER_LICENCE, ID -> matchIdentityDocument(user, extractedFields);
            case GRAY_CARD -> matchGrayCard(user, vehicle, extractedFields);
            case INSURANCE, USER_COMPANY_INSURANCE -> matchInsurance(documentType, user, vehicle, extractedFields);
            case USER_COMPANY_EXTRACT -> matchCompanyExtract(user, extractedFields);
            default -> new MatchingResult(DOCUMENT_MATCH_STATUS.NOT_APPLICABLE, null, Map.of("reason", "matching_not_defined_for_type"));
        };
    }

    private MatchingResult matchIdentityDocument(User user, Map<String, Object> extractedFields) {
        if (user == null) {
            return new MatchingResult(DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED, null, Map.of("reason", "missing_user"));
        }

        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(buildStringCheck("lastName", user.getLastName(), extractedFields.get("lastName")));
        checks.add(buildStringCheck("firstName", user.getFirstName(), extractedFields.get("firstName")));
        checks.add(buildDateCheck("birthDate", user.getBirthDate(), extractedFields.get("birthDate")));
        return summarizeChecks(checks);
    }

    private MatchingResult matchGrayCard(User user, Vehicle vehicle, Map<String, Object> extractedFields) {
        List<Map<String, Object>> checks = new ArrayList<>();
        if (vehicle != null) {
            checks.add(buildStringCheck("registrationNumber", vehicle.getRegistrationNumber(), extractedFields.get("registrationNumber")));
            checks.add(buildStringCheck("brand", vehicle.getBrand(), extractedFields.get("brand")));
            checks.add(buildStringCheck("model", vehicle.getModel(), extractedFields.get("model")));
        }
        if (user != null && extractedFields.get("holderName") != null) {
            checks.add(buildStringCheck("holderLastName", user.getLastName(), extractedFields.get("holderName")));
        }
        if (checks.isEmpty()) {
            return new MatchingResult(DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED, null, Map.of("reason", "missing_vehicle_profile_data"));
        }
        return summarizeChecks(checks);
    }

    private MatchingResult matchInsurance(DOCUMENT_TYPE documentType, User user, Vehicle vehicle, Map<String, Object> extractedFields) {
        List<Map<String, Object>> checks = new ArrayList<>();
        if (user != null && extractedFields.get("holderName") != null) {
            checks.add(buildStringCheck("holderName", user.getLastName(), extractedFields.get("holderName")));
        }
        checks.add(buildExpectedValueCheck("insuranceKind", expectedInsuranceKind(documentType), extractedFields.get("insuranceKind")));
        checks.add(buildPresenceCheck("expiryDate", extractedFields.get("expiryDate")));
        if (documentType == DOCUMENT_TYPE.INSURANCE && vehicle != null) {
            checks.add(buildStringCheck("registrationNumber", vehicle.getRegistrationNumber(), extractedFields.get("registrationNumber")));
            checks.add(buildStringCheck("brand", vehicle.getBrand(), extractedFields.get("brand")));
            checks.add(buildStringCheck("model", vehicle.getModel(), extractedFields.get("model")));
            checks.add(buildStringCheck("energyType", vehicle.getEnergyType(), extractedFields.get("energyType")));
        }
        if (documentType == DOCUMENT_TYPE.USER_COMPANY_INSURANCE) {
            checks.add(buildStringCheck("companyName", findCompanyExtractName(user), extractedFields.get("companyName")));
        }
        return summarizeChecks(checks);
    }

    private MatchingResult matchCompanyExtract(User user, Map<String, Object> extractedFields) {
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(buildPresenceCheck("companyName", extractedFields.get("companyName")));
        checks.add(buildPresenceCheck("siren", extractedFields.get("siren")));
        if (user != null) {
            checks.add(buildStringCheck("lastName", user.getLastName(), extractedFields.get("lastName")));
            checks.add(buildStringCheck("firstName", user.getFirstName(), extractedFields.get("firstName")));
        }
        return summarizeChecks(checks);
    }

    private MatchingResult summarizeChecks(List<Map<String, Object>> checks) {
        if (checks.isEmpty()) {
            return new MatchingResult(DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED, null, Map.of("reason", "no_matching_rules_applied"));
        }

        int matched = 0;
        int mismatched = 0;
        int missing = 0;
        double scoreSum = 0d;
        int scoreCount = 0;

        for (Map<String, Object> check : checks) {
            String status = Objects.toString(check.get("status"), "missing");
            if ("matched".equals(status)) {
                matched++;
            } else if ("mismatch".equals(status)) {
                mismatched++;
            } else {
                missing++;
            }
            Object score = check.get("score");
            if (score instanceof Number number) {
                scoreSum += number.doubleValue();
                scoreCount++;
            }
        }

        DOCUMENT_MATCH_STATUS status;
        if (mismatched > 0) {
            status = DOCUMENT_MATCH_STATUS.MISMATCH;
        } else if (matched > 0 && missing == 0) {
            status = DOCUMENT_MATCH_STATUS.MATCHED;
        } else if (matched > 0) {
            status = DOCUMENT_MATCH_STATUS.PARTIAL;
        } else {
            status = DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("checks", checks);
        details.put("matchedCount", matched);
        details.put("mismatchCount", mismatched);
        details.put("missingCount", missing);

        Double score = scoreCount == 0 ? null : scoreSum / scoreCount;
        return new MatchingResult(status, score, details);
    }

    private Map<String, Object> buildStringCheck(String field, Object expectedValue, Object actualValue) {
        String expected = normalizeText(expectedValue);
        String actual = normalizeText(actualValue);
        Map<String, Object> result = baseCheck(field, expectedValue, actualValue);
        if (expected == null || expected.isBlank() || actual == null || actual.isBlank()) {
            result.put("status", "missing");
            return result;
        }

        double score = stringSimilarity(expected, actual);
        result.put("score", score);
        result.put("status", score >= 0.84 ? "matched" : "mismatch");
        return result;
    }

    private Map<String, Object> buildDateCheck(String field, java.util.Date expectedDate, Object actualValue) {
        Map<String, Object> result = baseCheck(field, expectedDate, actualValue);
        LocalDate expected = toLocalDate(expectedDate);
        LocalDate actual = parseDate(actualValue);
        if (expected == null || actual == null) {
            result.put("status", "missing");
            return result;
        }
        boolean match = expected.equals(actual);
        result.put("score", match ? 1d : 0d);
        result.put("status", match ? "matched" : "mismatch");
        return result;
    }

    private Map<String, Object> buildPresenceCheck(String field, Object actualValue) {
        Map<String, Object> result = baseCheck(field, null, actualValue);
        boolean present = actualValue != null && !actualValue.toString().isBlank();
        result.put("score", present ? 1d : 0d);
        result.put("status", present ? "matched" : "missing");
        return result;
    }

    private Map<String, Object> buildExpectedValueCheck(String field, String expectedValue, Object actualValue) {
        Map<String, Object> result = baseCheck(field, expectedValue, actualValue);
        if (expectedValue == null || expectedValue.isBlank()) {
            result.put("status", "missing");
            return result;
        }
        String actual = actualValue == null ? null : actualValue.toString().trim();
        if (actual == null || actual.isBlank()) {
            result.put("status", "missing");
            return result;
        }
        boolean match = expectedValue.equalsIgnoreCase(actual);
        result.put("score", match ? 1d : 0d);
        result.put("status", match ? "matched" : "mismatch");
        return result;
    }

    private Map<String, Object> baseCheck(String field, Object expectedValue, Object actualValue) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("field", field);
        result.put("expected", expectedValue);
        result.put("actual", actualValue);
        return result;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value.toString(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT)
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    private LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        String raw = value.toString().trim().replace(' ', '/');
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate toLocalDate(java.util.Date value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String expectedInsuranceKind(DOCUMENT_TYPE documentType) {
        return switch (documentType) {
            case INSURANCE -> "VEHICLE";
            case USER_COMPANY_INSURANCE -> "COMPANY_TRANSPORT";
            default -> null;
        };
    }

    private String findCompanyExtractName(User user) {
        if (user == null || user.getDocument() == null) {
            return null;
        }
        return user.getDocument().stream()
                .filter(document -> document.getType() == DOCUMENT_TYPE.USER_COMPANY_EXTRACT)
                .map(this::extractCompanyNameFromDocument)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extractCompanyNameFromDocument(Document document) {
        if (document == null || document.getOcrExtractedData() == null || document.getOcrExtractedData().isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(document.getOcrExtractedData());
            JsonNode companyNameNode = root.path("fields").path("fields").path("companyName");
            return companyNameNode.isMissingNode() || companyNameNode.isNull() ? null : companyNameNode.asText(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double stringSimilarity(String expected, String actual) {
        if (expected.equals(actual)) {
            return 1d;
        }
        if (expected.contains(actual) || actual.contains(expected)) {
            return 0.9d;
        }
        int distance = levenshteinDistance(expected, actual);
        int maxLength = Math.max(expected.length(), actual.length());
        if (maxLength == 0) {
            return 1d;
        }
        return Math.max(0d, 1d - ((double) distance / maxLength));
    }

    private int levenshteinDistance(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            costs[0] = i;
            int previous = i - 1;
            for (int j = 1; j <= right.length(); j++) {
                int current = costs[j];
                int candidate = left.charAt(i - 1) == right.charAt(j - 1) ? previous : previous + 1;
                costs[j] = Math.min(Math.min(costs[j] + 1, costs[j - 1] + 1), candidate);
                previous = current;
            }
        }
        return costs[right.length()];
    }

    public record MatchingResult(DOCUMENT_MATCH_STATUS status, Double score, Map<String, Object> details) {
    }
}
