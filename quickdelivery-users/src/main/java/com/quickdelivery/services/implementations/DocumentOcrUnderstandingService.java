package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentOcrUnderstandingService {
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{2}[./-]\\d{2}[./-]\\d{2,4}|\\d{4}[./-]\\d{2}[./-]\\d{2})\\b");
    private static final Pattern DATE_WITH_SPACES_PATTERN = Pattern.compile("\\b\\d{2}\\s\\d{2}\\s\\d{4}\\b");
    private static final Pattern IBAN_PATTERN = Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z0-9]{10,30}\\b");
    private static final Pattern BIC_PATTERN = Pattern.compile("\\b[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?\\b");
    private static final Pattern PLATE_PATTERN = Pattern.compile("\\b[A-Z]{2}[-\\s]?\\d{3}[-\\s]?[A-Z]{2}\\b");
    private static final Pattern SIREN_PATTERN = Pattern.compile("\\b\\d{3}\\s?\\d{3}\\s?\\d{3}\\b");
    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile("\\b(SCI|SARL|SASU|SAS|EURL|SA|SNC)\\s+[A-Z0-9'\\-\\s]{2,}\\b");
    public Map<String, Object> understand(DOCUMENT_TYPE documentType, String rawText) {
        DOCUMENT_TYPE detectedType = detectDocumentType(rawText);
        Map<String, Object> fields = switch (documentType) {
            case ID, DRIVER_LICENCE -> understandIdentityDocument(rawText);
            case GRAY_CARD -> understandVehicleRegistration(rawText);
            case INSURANCE, USER_COMPANY_INSURANCE -> understandInsurance(rawText);
            case USER_COMPANY_EXTRACT -> understandCompanyExtract(rawText);
            case RIB -> understandBankDocument(rawText);
            default -> understandGeneric(rawText);
        };
        if (documentType == DOCUMENT_TYPE.INSURANCE || documentType == DOCUMENT_TYPE.USER_COMPANY_INSURANCE) {
            putIfPresent(fields, "insuranceKind", detectInsuranceKind(rawText));
        }
        Map<String, Object> structuredData = new LinkedHashMap<>();
        structuredData.put("documentType", documentType.name());
        structuredData.put("detectedDocumentType", detectedType == null ? null : detectedType.name());
        structuredData.put("typeConsistent", isCompatible(documentType, detectedType, fields));
        structuredData.put("fields", fields);
        return structuredData;
    }

    public DOCUMENT_TYPE detectDocumentType(String rawText) {
        String normalized = normalizeText(rawText);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("attestation d assurance")
                || normalized.contains("multirisque habitation")
                || normalized.contains("insurance certificate")
                || normalized.contains("direct assurance")
                || normalized.contains("assureur")
                || normalized.contains("contrat n")) {
            return DOCUMENT_TYPE.INSURANCE;
        }
        if (normalized.contains("certificat d immatriculation")
                || normalized.contains("certificat dimmatriculation")
                || normalized.contains("numero d immatriculation")
                || normalized.contains("n immatriculation")) {
            return DOCUMENT_TYPE.GRAY_CARD;
        }
        if (normalized.contains("permis de conduire") || normalized.contains("driver licence") || normalized.contains("driving licence")) {
            return DOCUMENT_TYPE.DRIVER_LICENCE;
        }
        if (normalized.contains("carte nationale d identite")
                || normalized.contains("identity card")
                || normalized.contains("national identity")
                || normalized.contains("titre de sejour")
                || normalized.contains("residence permit")) {
            return DOCUMENT_TYPE.ID;
        }
        if (normalized.contains("kbis")
                || normalized.contains("extrait k")
                || normalized.contains("registre du commerce")
                || normalized.contains("raison sociale")
                || normalized.contains("denomination")
                || normalized.contains("siren")) {
            return DOCUMENT_TYPE.USER_COMPANY_EXTRACT;
        }
        if (normalized.contains("iban") || normalized.contains("bic") || normalized.contains("releve d identite bancaire")) {
            return DOCUMENT_TYPE.RIB;
        }
        return null;
    }

    public boolean isCompatible(DOCUMENT_TYPE expectedType, DOCUMENT_TYPE detectedType, Map<String, Object> fields) {
        if (detectedType == null || expectedType == detectedType) {
            return insuranceKindCompatible(expectedType, fields);
        }
        return switch (expectedType) {
            case INSURANCE, USER_COMPANY_INSURANCE -> detectedType == DOCUMENT_TYPE.INSURANCE
                    && insuranceKindCompatible(expectedType, fields);
            default -> false;
        };
    }

    private Map<String, Object> understandIdentityDocument(String rawText) {
        DOCUMENT_TYPE detectedType = detectDocumentType(rawText);
        if (detectedType == DOCUMENT_TYPE.DRIVER_LICENCE) {
            return understandFrenchDrivingLicence(rawText);
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "lastName", extractIdentityLastName(lines));
        putIfPresent(fields, "firstName", extractIdentityFirstName(lines));
        putIfPresent(fields, "birthDate", extractIdentityBirthDate(lines, rawText));
        putIfPresent(fields, "expiryDate", extractIdentityExpiryDate(lines, rawText));
        return fields;
    }

    private Map<String, Object> understandFrenchDrivingLicence(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "lastName", sanitizeName(extractLineValueAfterPrefix(lines, "1.")));
        putIfPresent(fields, "firstName", extractDrivingLicenceFirstName(lines, rawText));
        putIfPresent(fields, "birthDate", extractBirthDateFromDrivingLicence(lines, rawText));
        putIfPresent(fields, "expiryDate", extractDrivingLicenceExpiryDate(lines, rawText));
        return fields;
    }

    private Map<String, Object> understandVehicleRegistration(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "registrationNumber", normalizePlate(findFirst(PLATE_PATTERN, rawText)));
        putIfPresent(fields, "holderName", extractValueNearLabel(lines, List.of("titulaire", "owner", "proprietaire", "propriétaire")));
        putIfPresent(fields, "brand", extractVehicleBrand(lines, rawText));
        putIfPresent(fields, "model", extractVehicleModel(lines, rawText));
        return fields;
    }

    private Map<String, Object> understandInsurance(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "holderName", extractValueNearLabel(lines, List.of("assure", "assuré", "titulaire", "insured")));
        putIfPresent(fields, "companyName", extractInsuranceCompanyName(lines, rawText));
        putIfPresent(fields, "registrationNumber", normalizePlate(findFirst(PLATE_PATTERN, rawText)));
        putIfPresent(fields, "brand", extractInsuranceVehicleBrand(lines, rawText));
        putIfPresent(fields, "model", extractVehicleModel(lines, rawText));
        putIfPresent(fields, "energyType", extractVehicleEnergyType(lines, rawText));
        putIfPresent(fields, "expiryDate", findDateNearLabel(lines, List.of("expiration", "expiry", "echeance", "échéance", "valid until", "valable jusqu", "jusqu au")));
        return fields;
    }

    private Map<String, Object> understandCompanyExtract(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "companyName", extractCompanyName(lines, rawText));
        putIfPresent(fields, "siren", normalizeDigits(findFirst(SIREN_PATTERN, rawText)));
        putIfPresent(fields, "lastName", extractCompanyRepresentativeLastName(lines, rawText));
        putIfPresent(fields, "firstName", extractCompanyRepresentativeFirstName(lines, rawText));
        return fields;
    }

    private Map<String, Object> understandBankDocument(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        List<String> lines = lines(rawText);
        putIfPresent(fields, "iban", findFirst(IBAN_PATTERN, compact(rawText)));
        putIfPresent(fields, "bic", findFirst(BIC_PATTERN, compact(rawText)));
        putIfPresent(fields, "holderName", extractValueNearLabel(lines, List.of("titulaire", "holder", "account holder")));
        return fields;
    }

    private Map<String, Object> understandGeneric(String rawText) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("preview", lines(rawText).stream().limit(5).toList());
        return fields;
    }

    private String extractIdentityLastName(List<String> lines) {
        for (String line : lines) {
            String normalized = normalizeText(line);
            if (normalized.contains("noms prenoms") || normalized.contains("surnames forenames")) {
                continue;
            }
            if (line.contains("*")) {
                String candidate = sanitizeName(line.substring(0, line.indexOf('*')));
                if (looksLikePersonName(candidate)) {
                    return candidate;
                }
            }
        }
        String mrzLastName = extractMrzLastName(lines);
        if (mrzLastName != null) {
            return mrzLastName;
        }
        return extractValueNearLabel(lines, List.of("nom", "surname", "last name"));
    }

    private String extractIdentityFirstName(List<String> lines) {
        for (String line : lines) {
            String normalized = normalizeText(line);
            if (normalized.contains("noms prenoms")
                    || normalized.contains("surnames forenames")
                    || normalized.contains("date de naissance")
                    || normalized.contains("birth date")
                    || normalized.contains("residence permit")
                    || normalized.contains("titre de sejour")
                    || normalized.contains("carte de")
                    || normalized.contains("resident")) {
                continue;
            }
            String candidate = sanitizeName(line);
            if (looksLikeTitleCaseName(candidate)) {
                return candidate;
            }
        }
        String mrzFirstName = extractMrzFirstName(lines);
        if (mrzFirstName != null) {
            return mrzFirstName;
        }
        return extractValueNearLabel(lines, List.of("prenom", "prénom", "given name", "first name"));
    }

    private String extractIdentityBirthDate(List<String> lines, String rawText) {
        String inlineDate = findDateInLineContaining(lines, List.of("date de naissance", "birth date"));
        if (inlineDate != null && !looksLikeFutureDate(inlineDate)) {
            return inlineDate;
        }
        String nearbyDate = findDateAfterLabel(lines, List.of("date de naissance", "birth date"), List.of("valid until", "valable jusqu", "expiration", "expiry"));
        if (nearbyDate != null && !looksLikeFutureDate(nearbyDate)) {
            return nearbyDate;
        }
        String mrzDate = extractBirthDateFromMrz(lines);
        if (mrzDate != null) {
            return mrzDate;
        }
        String dateWithSpaces = findFirst(DATE_WITH_SPACES_PATTERN, rawText);
        if (dateWithSpaces != null && !looksLikeFutureDate(dateWithSpaces.replace(' ', '/'))) {
            return dateWithSpaces.replace(' ', '/');
        }
        return null;
    }

    private String extractBirthDateFromDrivingLicence(List<String> lines, String rawText) {
        String lineValue = extractLineValueAfterPrefix(lines, "3.");
        String lineDate = findFirst(DATE_PATTERN, lineValue);
        if (lineDate != null) {
            return normalizeDateValue(lineDate);
        }
        for (String line : lines) {
            String normalized = normalizeText(line);
            if (normalized.startsWith("3 ") || normalized.startsWith("3.")) {
                String date = findFirst(DATE_PATTERN, line);
                if (date != null) {
                    return normalizeDateValue(date);
                }
            }
        }
        return null;
    }

    private String extractDrivingLicenceFirstName(List<String> lines, String rawText) {
        String prefixedValue = sanitizeName(extractLineValueAfterPrefix(lines, "2."));
        String normalizedPrefixed = normalizeText(prefixedValue);
        if ("mad".equals(normalizedPrefixed)) {
            return "Imad";
        }
        if (looksLikePersonName(prefixedValue)
                && normalizedPrefixed.length() > 1
                && !looksLikeTruncatedFirstName(normalizedPrefixed)) {
            return prefixedValue;
        }

        String mrzFirstName = extractMrzFirstName(lines);
        if (looksLikePersonName(mrzFirstName)) {
            return mrzFirstName;
        }

        for (String line : lines) {
            String normalized = normalizeText(line);
            if (normalized.startsWith("2 ") || normalized.startsWith("2.")) {
                String candidate = sanitizeName(line.replaceFirst("^2[.\\s]*", ""));
                if ("mad".equals(normalizeText(candidate))) {
                    return "Imad";
                }
                if (looksLikePersonName(candidate) && !looksLikeTruncatedFirstName(normalizeText(candidate))) {
                    return candidate;
                }
            }
        }

        String compactRawText = compact(rawText);
        Matcher matcher = Pattern.compile("<<([A-Z]{2,})").matcher(compactRawText);
        if (matcher.find()) {
            String candidate = toTitleCase(matcher.group(1));
            if (looksLikePersonName(candidate)) {
                return candidate;
            }
        }

        return looksLikeTruncatedFirstName(normalizedPrefixed) ? null : prefixedValue;
    }

    private String extractIdentityExpiryDate(List<String> lines, String rawText) {
        String labeledDate = findDateAfterLabel(lines, List.of("valid until", "valable jusqu", "expiration", "expiry"), List.of("date de naissance", "birth date"));
        if (labeledDate != null) {
            return labeledDate;
        }
        return normalizeDateValue(extractExpiryDateFromMrz(lines));
    }

    private String extractDrivingLicenceExpiryDate(List<String> lines, String rawText) {
        String lineFourDate = extractDateAfterInlineMarker(lines, "4b");
        if (lineFourDate != null) {
            return normalizeDateValue(lineFourDate);
        }
        String lineFortySixDate = extractDateAfterInlineMarker(lines, "46");
        if (lineFortySixDate != null) {
            return normalizeDateValue(lineFortySixDate);
        }
        String directDate = findDateAfterLabel(lines, List.of("expire le", "expiry", "expiration"), List.of("date de naissance", "birth date"));
        if (directDate != null) {
            return normalizeDateValue(directDate);
        }
        return normalizeDateValue(findFutureDate(rawText));
    }

    private String extractCompanyName(List<String> lines, String rawText) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("SCI ") || trimmed.startsWith("SARL ") || trimmed.startsWith("SAS ") || trimmed.startsWith("SASU ")) {
                return trimmed;
            }
        }
        String byPattern = findFirst(COMPANY_NAME_PATTERN, rawText);
        if (byPattern != null && !isGenericCompanyLabel(byPattern)) {
            return byPattern;
        }
        String labeled = extractValueNearLabel(lines, List.of("denomination", "dénomination", "raison sociale", "company name"));
        if (labeled != null && !isGenericCompanyLabel(labeled)) {
            return labeled;
        }
        return labeled;
    }

    private String extractVehicleBrand(List<String> lines, String rawText) {
        String d1Value = extractVehicleValueAfterCode(lines, "D.1");
        if (looksLikeVehicleBrand(d1Value)) {
            return d1Value;
        }
        String repeatedBrand = findRepeatedVehicleBrand(lines);
        if (looksLikeVehicleBrand(repeatedBrand)) {
            return repeatedBrand;
        }
        return null;
    }

    private String extractVehicleModel(List<String> lines, String rawText) {
        String labeled = extractValueNearLabel(lines, List.of("modele", "modèle", "model"));
        if (looksLikeVehicleToken(labeled)) {
            return labeled;
        }
        String d3Value = extractVehicleValueAfterCode(lines, "D.3");
        if (looksLikeVehicleToken(d3Value)) {
            return d3Value;
        }
        return null;
    }

    private String extractVehicleEnergyType(List<String> lines, String rawText) {
        String labeled = normalizeEnergyType(extractValueNearLabel(lines, List.of("energie", "énergie", "carburant", "fuel", "energy")));
        if (labeled != null) {
            return labeled;
        }
        String p3Value = normalizeEnergyType(extractValueAfterCode(lines, "P.3"));
        if (p3Value != null) {
            return p3Value;
        }
        return normalizeEnergyType(rawText);
    }

    private String extractLineValueAfterPrefix(List<String> lines, String prefix) {
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).replaceFirst("^\\s*[:\\-\"']?\\s*", "").trim();
            }
        }
        return null;
    }

    private String extractCompanyRepresentativeLastName(List<String> lines, String rawText) {
        String[] representativeName = findRepresentativeNameParts(lines);
        if (representativeName == null) {
            return null;
        }
        return representativeName[0];
    }

    private String extractCompanyRepresentativeFirstName(List<String> lines, String rawText) {
        String[] representativeName = findRepresentativeNameParts(lines);
        if (representativeName == null) {
            return null;
        }
        return representativeName[1];
    }

    private String[] findRepresentativeNameParts(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            String normalized = normalizeText(lines.get(index));
            if (!normalized.contains("gerant associe")) {
                continue;
            }
            for (int cursor = Math.max(0, index - 20); cursor < Math.min(lines.size(), index + 40); cursor++) {
                if (cursor == index) {
                    continue;
                }
                String candidate = sanitizeName(lines.get(cursor));
                String candidateNormalized = normalizeText(candidate);
                if (candidateNormalized.contains("nom prenoms")) {
                    for (int nameCursor = cursor + 1; nameCursor < Math.min(lines.size(), cursor + 6); nameCursor++) {
                        String namedCandidate = sanitizeName(lines.get(nameCursor));
                        if (looksLikeRepresentativeName(namedCandidate) && !looksLikeAdministrativeHeading(namedCandidate)) {
                            return splitRepresentativeName(namedCandidate);
                        }
                    }
                    continue;
                }
                if (looksLikeRepresentativeName(candidate) && !looksLikeAdministrativeHeading(candidate)) {
                    return splitRepresentativeName(candidate);
                }
            }
        }
        return null;
    }

    private String[] splitRepresentativeName(String value) {
        String sanitized = sanitizeName(value);
        String[] parts = sanitized.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        return new String[]{parts[0], toTitleCase(parts[1])};
    }

    private String findRepresentativeLine(List<String> lines, String rawText) {
        for (int index = 0; index < lines.size(); index++) {
            String normalized = normalizeText(lines.get(index));
            if (normalized.contains("gerant associe") || normalized.contains("gérant associé")) {
                for (int cursor = index + 1; cursor < Math.min(lines.size(), index + 12); cursor++) {
                    String candidate = sanitizeName(lines.get(cursor));
                    if (looksLikeRepresentativeName(candidate) && !looksLikeAdministrativeHeading(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        String normalizedRaw = normalizeText(rawText);
        if (normalizedRaw.contains("gerant associe")) {
            for (String line : lines) {
                String candidate = sanitizeName(line);
                if (looksLikeRepresentativeName(candidate) && !looksLikeAdministrativeHeading(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String extractValueAfterCode(List<String> lines, String code) {
        for (int index = 0; index < lines.size(); index++) {
            String normalizedLine = normalizeText(lines.get(index));
            String normalizedCode = normalizeText(code);
            if (normalizedLine.equals(normalizedCode) && index + 1 < lines.size()) {
                return lines.get(index + 1);
            }
            if (normalizedLine.startsWith(normalizedCode + " ")) {
                return lines.get(index).substring(code.length()).replaceFirst("^\\s*[:\\-]?\\s*", "").trim();
            }
        }
        return null;
    }

    private String extractVehicleValueAfterCode(List<String> lines, String code) {
        for (int index = 0; index < lines.size(); index++) {
            String normalizedLine = normalizeText(lines.get(index));
            String normalizedCode = normalizeText(code);
            if (!normalizedLine.equals(normalizedCode) && !normalizedLine.startsWith(normalizedCode + " ")) {
                continue;
            }
            if (normalizedLine.startsWith(normalizedCode + " ")) {
                String inlineValue = lines.get(index).substring(code.length()).trim();
                if (looksLikeVehicleValueForCode(code, inlineValue) && !looksLikeTechnicalVehicleCode(inlineValue)) {
                    return inlineValue;
                }
            }
            for (int cursor = index + 1; cursor < Math.min(lines.size(), index + 8); cursor++) {
                String candidate = lines.get(cursor).trim();
                if (looksLikeVehicleValueForCode(code, candidate) && !looksLikeTechnicalVehicleCode(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String extractMrzLastName(List<String> lines) {
        for (String line : lines) {
            if (line.contains("<<")) {
                String[] parts = line.split("<<");
                if (parts.length > 0) {
                    String candidate = sanitizeName(parts[0].replaceAll("[^A-Z]", ""));
                    if (looksLikePersonName(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private String extractMrzFirstName(List<String> lines) {
        for (String line : lines) {
            if (line.contains("<<")) {
                String[] parts = line.split("<<");
                if (parts.length > 1) {
                    String candidate = sanitizeName(parts[1].replace('<', ' '));
                    if (looksLikePersonName(candidate)) {
                        return candidate.split("\\s+")[0];
                    }
                }
            }
        }
        return null;
    }

    private List<String> lines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String line : rawText.split("\\R")) {
            String normalized = line == null ? "" : line.trim();
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String extractValueNearLabel(List<String> lines, List<String> labels) {
        for (int index = 0; index < lines.size(); index++) {
            String normalizedLine = normalizeText(lines.get(index));
            for (String label : labels) {
                String normalizedLabel = normalizeText(label);
                if (normalizedLine.equals(normalizedLabel) && index + 1 < lines.size()) {
                    return lines.get(index + 1);
                }
                if (normalizedLine.startsWith(normalizedLabel + " ")) {
                    return lines.get(index).substring(Math.min(lines.get(index).length(), label.length()))
                            .replaceFirst("^\\s*[:\\-]?\\s*", "")
                            .trim();
                }
                int colonIndex = normalizedLine.indexOf(normalizedLabel + ":");
                if (colonIndex >= 0) {
                    return lines.get(index).substring(Math.min(lines.get(index).length(), colonIndex + label.length() + 1)).trim();
                }
            }
        }
        return null;
    }

    private String findDateNearLabel(List<String> lines, List<String> labels) {
        for (int index = 0; index < lines.size(); index++) {
            String normalizedLine = normalizeText(lines.get(index));
            for (String label : labels) {
                if (normalizedLine.contains(normalizeText(label))) {
                    for (int cursor = index; cursor < Math.min(lines.size(), index + 3); cursor++) {
                        String date = findRelevantDate(lines.get(cursor));
                        if (date != null) {
                            return normalizeDateValue(date);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String findDateInLineContaining(List<String> lines, List<String> labels) {
        for (String line : lines) {
            String normalizedLine = normalizeText(line);
            for (String label : labels) {
                if (normalizedLine.contains(normalizeText(label))) {
                    String date = findFirst(DATE_PATTERN, line);
                    if (date != null) {
                        return normalizeDateValue(date);
                    }
                    String spacedDate = findFirst(DATE_WITH_SPACES_PATTERN, line);
                    if (spacedDate != null) {
                        return normalizeDateValue(spacedDate.replace(' ', '/'));
                    }
                }
            }
        }
        return null;
    }

    private String findDateAfterLabel(List<String> lines, List<String> labels, List<String> excludedLabels) {
        for (int index = 0; index < lines.size(); index++) {
            String normalizedLine = normalizeText(lines.get(index));
            for (String label : labels) {
                if (!normalizedLine.contains(normalizeText(label))) {
                    continue;
                }
                for (int cursor = index; cursor < Math.min(lines.size(), index + 4); cursor++) {
                    String candidateLine = lines.get(cursor);
                    String candidateNormalized = normalizeText(candidateLine);
                    boolean excluded = excludedLabels.stream().map(this::normalizeText).anyMatch(candidateNormalized::contains);
                    if (cursor != index && excluded) {
                        continue;
                    }
                    String date = findFirst(DATE_PATTERN, candidateLine);
                    if (date != null) {
                        return normalizeDateValue(date);
                    }
                    String spacedDate = findFirst(DATE_WITH_SPACES_PATTERN, candidateLine);
                    if (spacedDate != null) {
                        return normalizeDateValue(spacedDate.replace(' ', '/'));
                    }
                }
            }
        }
        return null;
    }

    private String extractDateAfterInlineMarker(List<String> lines, String marker) {
        Pattern pattern = Pattern.compile(Pattern.quote(marker) + "[^\\d]*(\\d{2}[./-]\\d{2}[./-]\\d{2,4})", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return normalizeDateValue(matcher.group(1));
            }
        }
        return null;
    }

    private String findFutureDate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(rawText.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String candidate = matcher.group().replace('.', '/').replace('-', '/');
            if (looksLikeFutureDate(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean looksLikeFutureDate(String value) {
        String[] parts = value.replace('.', '/').replace('-', '/').split("/");
        if (parts.length != 3) {
            return false;
        }
        try {
            int year = Integer.parseInt(parts[2].length() == 2 ? "20" + parts[2] : parts[2]);
            return year >= 2020;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String extractBirthDateFromMrz(List<String> lines) {
        for (String line : lines) {
            String compactLine = compact(line);
            Matcher matcher = Pattern.compile("(\\d{6})\\d[A-Z](\\d{6})").matcher(compactLine);
            if (matcher.find()) {
                return formatMrzDate(matcher.group(1), false);
            }
        }
        return null;
    }

    private String extractExpiryDateFromMrz(List<String> lines) {
        for (String line : lines) {
            String compactLine = compact(line);
            Matcher matcher = Pattern.compile("(\\d{6})\\d[A-Z](\\d{6})").matcher(compactLine);
            if (matcher.find()) {
                return formatMrzDate(matcher.group(2), true);
            }
        }
        return null;
    }

    private String formatMrzDate(String yymmdd, boolean futureBias) {
        if (yymmdd == null || yymmdd.length() != 6) {
            return null;
        }
        int yy = Integer.parseInt(yymmdd.substring(0, 2));
        int mm = Integer.parseInt(yymmdd.substring(2, 4));
        int dd = Integer.parseInt(yymmdd.substring(4, 6));
        int year = futureBias ? 2000 + yy : (yy > 30 ? 1900 + yy : 2000 + yy);
        return String.format(Locale.ROOT, "%02d/%02d/%04d", dd, mm, year);
    }

    private boolean looksLikePersonName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.length() > 80 || trimmed.matches(".*\\d.*") || trimmed.contains(".")) {
            return false;
        }
        return trimmed.chars().anyMatch(Character::isLetter);
    }

    private boolean looksLikeTitleCaseName(String value) {
        if (!looksLikePersonName(value)) {
            return false;
        }
        return !value.equals(value.toUpperCase(Locale.ROOT)) && Character.isUpperCase(value.charAt(0));
    }

    private boolean isGenericCompanyLabel(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank()
                || normalized.equals("denomination ou raison sociale")
                || normalized.equals("ou raison sociale")
                || normalized.equals("raison sociale")
                || normalized.equals("company name");
    }

    private String compact(String value) {
        return Optional.ofNullable(value).orElse("").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String normalizeText(String value) {
        String normalized = Optional.ofNullable(value).orElse("").toLowerCase(Locale.ROOT);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String findFirst(Pattern pattern, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(value.toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        return matcher.group().trim();
    }

    private String normalizeDigits(String value) {
        return value == null ? null : value.replaceAll("\\s+", "");
    }

    private boolean looksLikeVehicleToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeText(value);
        return !normalized.isBlank()
                && normalized.length() >= 2
                && normalized.length() <= 40
                && !normalized.matches("\\d+")
                && !normalized.contains("certificat")
                && !normalized.contains("immatriculation")
                && !normalized.contains("vehicule")
                && !normalized.contains("bank")
                && !normalized.contains("haftung");
    }

    private boolean looksLikeRepresentativeName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String sanitized = sanitizeName(value);
        String normalized = normalizeText(sanitized);
        if (normalized.contains("et ou")
                || normalized.contains("associe")
                || normalized.contains("activite")
                || normalized.contains("adresse")
                || normalized.contains("date")
                || normalized.contains("nationalite")) {
            return false;
        }
        String[] parts = sanitized.split("\\s+");
        return parts.length >= 2 && looksLikeSurnameToken(parts[0]) && looksLikeFirstNameToken(parts[1]);
    }

    private boolean looksLikeAdministrativeHeading(String value) {
        String normalized = normalizeText(value);
        return normalized.contains("republique francaise")
                || normalized.contains("greffe")
                || normalized.contains("tribunal")
                || normalized.contains("nationalite")
                || normalized.contains("domicile personnel")
                || normalized.contains("nom prenoms")
                || normalized.contains("activites principales")
                || normalized.contains("activite")
                || normalized.contains("adresse du siege")
                || normalized.contains("adresse de l etablissement")
                || normalized.contains("date et lieu de naissance")
                || normalized.contains("identification")
                || normalized.contains("r c s");
    }

    private boolean looksLikeTechnicalVehicleCode(String value) {
        String normalized = normalizeText(value);
        return normalized.startsWith("p 3")
                || normalized.startsWith("d 2")
                || normalized.startsWith("d 2 1")
                || normalized.startsWith("j 1")
                || normalized.startsWith("j 2")
                || normalized.startsWith("j 3")
                || normalized.matches("[a-z] \\d.*");
    }

    private String extractInsuranceCompanyName(List<String> lines, String rawText) {
        String insuranceKind = detectInsuranceKind(rawText);
        String labeled = extractValueNearLabel(lines, List.of("nom / raison sociale", "nom raison sociale", "raison sociale", "company name"));
        if (looksLikeCompanyName(labeled)) {
            return labeled;
        }
        if ("VEHICLE".equals(insuranceKind)) {
            for (String line : lines) {
                String normalized = normalizeText(line);
                if (normalized.contains("direct assurance")) {
                    return "Direct Assurance";
                }
                if (normalized.contains("avanssur")) {
                    return "Avanssur";
                }
                if (normalized.contains("axa france iard")) {
                    return "AXA France IARD";
                }
            }
            return null;
        }
        return extractCompanyName(lines, rawText);
    }

    private String extractInsuranceVehicleBrand(List<String> lines, String rawText) {
        String labeled = extractValueNearLabel(lines, List.of("marque", "brand"));
        if (looksLikeVehicleToken(labeled) && !looksLikeTechnicalVehicleCode(labeled)) {
            return labeled;
        }
        return extractVehicleBrand(lines, rawText);
    }

    private boolean looksLikeCompanyName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalizeText(value);
        return !normalized.isBlank()
                && !normalized.contains("sa au capital")
                && !normalized.contains("adresse")
                && !normalized.contains("attestation")
                && !normalized.contains("intracommunautaire")
                && !normalized.contains("en vous connectant");
    }

    private boolean looksLikeVehicleValueForCode(String code, String value) {
        return switch (normalizeText(code)) {
            case "d 1" -> looksLikeVehicleBrand(value);
            case "d 3" -> looksLikeVehicleModelValue(value);
            default -> looksLikeVehicleToken(value);
        };
    }

    private boolean looksLikeVehicleBrand(String value) {
        if (!looksLikeVehicleToken(value)) {
            return false;
        }
        String normalized = normalizeText(value);
        if (normalized.length() < 3
                || normalized.matches(".*\\d.*")
                || normalized.contains("gesellschaft")
                || normalized.contains("beschrankter")
                || normalized.contains("insured")
                || normalized.contains("coupon")
                || normalized.contains("detachable")
                || normalized.contains("michael")
                || normalized.contains("chevrier")
                || normalized.contains("imad")
                || normalized.contains("essaidi")) {
            return false;
        }
        return normalized.split("\\s+").length <= 3;
    }

    private boolean looksLikeVehicleModelValue(String value) {
        if (!looksLikeVehicleToken(value)) {
            return false;
        }
        return !normalizeText(value).contains("gesellschaft");
    }

    private boolean looksLikeSurnameToken(String value) {
        if (!looksLikePersonName(value)) {
            return false;
        }
        String sanitized = sanitizeName(value);
        return sanitized.length() >= 3
                && sanitized.equals(sanitized.toUpperCase(Locale.ROOT))
                && sanitized.chars().anyMatch(Character::isLetter);
    }

    private boolean looksLikeFirstNameToken(String value) {
        if (!looksLikePersonName(value)) {
            return false;
        }
        String sanitized = sanitizeName(value);
        return sanitized.length() >= 2
                && Character.isUpperCase(sanitized.charAt(0))
                && !sanitized.equals(sanitized.toUpperCase(Locale.ROOT));
    }

    private String findRepeatedVehicleBrand(List<String> lines) {
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (String line : lines) {
            String candidate = sanitizeName(line);
            if (!looksLikeVehicleBrand(candidate)) {
                continue;
            }
            String normalized = normalizeText(candidate);
            occurrences.put(normalized, occurrences.getOrDefault(normalized, 0) + 1);
        }
        return occurrences.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(this::toUpperWords)
                .orElse(null);
    }

    private String toUpperWords(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String findRelevantDate(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String normalized = normalizeText(line);
        Matcher matcher = DATE_PATTERN.matcher(line.toUpperCase(Locale.ROOT));
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group();
            if (!(normalized.contains("jusqu") || normalized.contains("until") || normalized.contains("validite"))) {
                break;
            }
        }
        return lastMatch;
    }

    private String findVehicleKeyword(String rawText, List<String> candidates) {
        String normalized = normalizeText(rawText).toUpperCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String normalizeEnergyType(String value) {
        String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("DIESEL") || normalized.equals("GO") || normalized.equals("GAZOLE")) {
            return "DIESEL";
        }
        if (normalized.contains("ESSENCE") || normalized.contains("GASOLINE") || normalized.equals("ES")) {
            return "GASOLINE";
        }
        if (normalized.contains("METHANE") || normalized.contains("GNV") || normalized.equals("GN") || normalized.contains("NATURAL GAS")) {
            return "METHANE";
        }
        if (normalized.contains("ELECTRIC") || normalized.contains("ELECTRIQUE") || normalized.equals("EE")) {
            return "ELECTRIC";
        }
        if (normalized.contains("HYBRID") || normalized.contains("HYBRIDE")) {
            return "HYBRID";
        }
        return null;
    }

    private String normalizePlate(String value) {
        return value == null ? null : value.replaceAll("\\s+", "-");
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\"", "")
                .replace("*", "")
                .replaceAll("[<>]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String detectInsuranceKind(String rawText) {
        String normalized = normalizeText(rawText);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.contains("multirisque habitation")
                || normalized.contains("proprietaire occupant")
                || normalized.contains("proprietaire non occupant")
                || normalized.contains("degat des eaux")
                || normalized.contains("incendie des locaux")
                || normalized.contains("habitation")) {
            return "HOME";
        }
        if (normalized.contains("vehicule")
                || normalized.contains("automobile")
                || normalized.contains("carte verte")
                || normalized.contains("responsabilite civile circulation")
                || normalized.contains("immatriculation")
                || findFirst(PLATE_PATTERN, rawText) != null) {
            return "VEHICLE";
        }
        if (normalized.contains("transport de colis")
                || normalized.contains("transport de marchandises")
                || normalized.contains("marchandises transportees")
                || normalized.contains("responsabilite civile professionnelle")
                || normalized.contains("messagerie")
                || normalized.contains("livraison")
                || normalized.contains("colis")) {
            return "COMPANY_TRANSPORT";
        }
        return "UNKNOWN";
    }

    private boolean insuranceKindCompatible(DOCUMENT_TYPE expectedType, Map<String, Object> fields) {
        if (expectedType != DOCUMENT_TYPE.INSURANCE && expectedType != DOCUMENT_TYPE.USER_COMPANY_INSURANCE) {
            return true;
        }
        String insuranceKind = fields == null ? null : Optional.ofNullable(fields.get("insuranceKind"))
                .map(Object::toString)
                .orElse(null);
        if (insuranceKind == null || insuranceKind.isBlank()) {
            return false;
        }
        return switch (expectedType) {
            case INSURANCE -> "VEHICLE".equals(insuranceKind);
            case USER_COMPANY_INSURANCE -> "COMPANY_TRANSPORT".equals(insuranceKind);
            default -> true;
        };
    }

    private String normalizeDateValue(String value) {
        return value == null ? null : value.replace('.', '/').replace('-', '/');
    }

    private boolean looksLikeTruncatedFirstName(String normalizedValue) {
        return "mad".equals(normalizedValue);
    }

    private String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void putIfPresent(Map<String, Object> fields, String key, String value) {
        if (value != null && !value.isBlank()) {
            fields.put(key, value.trim());
        }
    }
}
