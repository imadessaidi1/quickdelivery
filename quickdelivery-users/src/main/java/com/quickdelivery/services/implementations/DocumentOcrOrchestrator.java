package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_OCR_STATUS;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.services.interfaces.DocumentOcrService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentOcrOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(DocumentOcrOrchestrator.class);
    private final Documents documents;
    private final DocumentOcrService documentOcrService;
    private final ObjectMapper objectMapper;
    private final DocumentProfileMatchingService documentProfileMatchingService;
    private final MeterRegistry meterRegistry;
    private final UserDocumentStorageService userDocumentStorageService;

    public DocumentOcrOrchestrator(Documents documents,
                                   DocumentOcrService documentOcrService,
                                   ObjectMapper objectMapper,
                                   DocumentProfileMatchingService documentProfileMatchingService,
                                   MeterRegistry meterRegistry,
                                   UserDocumentStorageService userDocumentStorageService) {
        this.documents = documents;
        this.documentOcrService = documentOcrService;
        this.objectMapper = objectMapper;
        this.documentProfileMatchingService = documentProfileMatchingService;
        this.meterRegistry = meterRegistry;
        this.userDocumentStorageService = userDocumentStorageService;
    }

    @Async("ocrTaskExecutor")
    public void processDocumentAsync(Long documentId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String result = "failed";
        logger.info("Starting OCR processing for document {}", documentId);
        Document document = documents.findByIdWithMatchingContext(documentId).orElse(null);
        if (document == null) {
            logger.warn("OCR document {} not found", documentId);
            result = "not_found";
            return;
        }

        if (document.getDocURL() == null || document.getDocURL().isBlank()) {
            markDisabled(document, "NO_DOCUMENT_PATH");
            result = "disabled";
            return;
        }

        if (!documentOcrService.isEnabled()) {
            markDisabled(document, "OCR_DISABLED");
            result = "disabled";
            return;
        }

        if (!userDocumentStorageService.exists(document.getDocURL())) {
            markFailure(document, "FILE_NOT_READY");
            result = "failed";
            return;
        }

        try {
            document.setOcrStatus(DOCUMENT_OCR_STATUS.PROCESSING);
            document.setOcrProvider(documentOcrService.providerName());
            document.setOcrErrorCode(null);
            document = documents.save(document);

            DocumentOcrService.OcrExtractionResult extractionResult = documentOcrService.extract(document);
            if (!extractionResult.success()) {
                if (extractionResult.errorCode() != null
                        && (extractionResult.errorCode().startsWith("OCR_NOT_APPLICABLE")
                        || extractionResult.errorCode().startsWith("OCR_MANUAL_REVIEW"))) {
                    markDisabled(document, extractionResult.errorCode());
                    return;
                }
                markFailure(document, extractionResult.errorCode());
                return;
            }

            String extractedData = objectMapper.writeValueAsString(Map.of(
                    "provider", documentOcrService.providerName(),
                    "text", extractionResult.rawText(),
                    "fields", extractionResult.structuredData()
            ));
            DocumentProfileMatchingService.MatchingResult matchingResult =
                    documentProfileMatchingService.match(document, extractionResult.structuredData());
            String matchDetails = objectMapper.writeValueAsString(matchingResult.details());
            document.setOcrStatus(DOCUMENT_OCR_STATUS.COMPLETED);
            document.setOcrProvider(documentOcrService.providerName());
            document.setOcrConfidenceScore(extractionResult.confidenceScore());
            document.setOcrExtractedData(extractedData);
            applyNormalizedOcrFields(document, extractionResult.structuredData());
            document.setOcrProcessedAt(new Date());
            document.setOcrErrorCode(extractionResult.errorCode());
            document.setMatchStatus(matchingResult.status());
            document.setMatchScore(matchingResult.score());
            document.setMatchDetails(matchDetails);
            document = documents.save(document);
            result = "completed";
            logger.info("OCR processing completed for document {} with provider {}", documentId, documentOcrService.providerName());
        } catch (Exception exception) {
            logger.warn("OCR processing failed for document {}: {}", documentId, exception.getMessage(), exception);
            markFailure(document, "OCR_PROCESSING_ERROR");
            result = "failed";
        } finally {
            sample.stop(Timer.builder("quickdelivery.ocr.document.duration")
                    .tag("result", result)
                    .register(meterRegistry));
            meterRegistry.counter("quickdelivery.ocr.document.processed", "result", result).increment();
        }
    }

    private void markDisabled(Document document, String errorCode) {
        Long documentId = document == null ? null : document.getId();
        document = reloadDocument(document);
        if (document == null) {
            logger.warn("Unable to mark OCR disabled because document {} no longer exists", documentId);
            return;
        }
        document.setOcrStatus(DOCUMENT_OCR_STATUS.DISABLED);
        document.setOcrProvider(documentOcrService.providerName());
        document.setOcrErrorCode(errorCode);
        document.setOcrProcessedAt(new Date());
        document.setMatchStatus(errorCode.startsWith("OCR_MANUAL_REVIEW") ? DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED : DOCUMENT_MATCH_STATUS.NOT_APPLICABLE);
        document.setMatchScore(null);
        document.setMatchDetails(null);
        documents.save(document);
        logger.info("OCR disabled for document {} with code {}", document.getId(), errorCode);
    }

    private void markFailure(Document document, String errorCode) {
        Long documentId = document == null ? null : document.getId();
        document = reloadDocument(document);
        if (document == null) {
            logger.warn("Unable to mark OCR failure because document {} no longer exists", documentId);
            return;
        }
        document.setOcrStatus(DOCUMENT_OCR_STATUS.FAILED);
        document.setOcrProvider(documentOcrService.providerName());
        document.setOcrErrorCode(errorCode);
        document.setOcrProcessedAt(new Date());
        document.setMatchStatus(DOCUMENT_MATCH_STATUS.UNAVAILABLE);
        document.setMatchScore(null);
        document.setMatchDetails(null);
        documents.save(document);
        logger.warn("OCR failed for document {} with code {}", document.getId(), errorCode);
    }

    private Document reloadDocument(Document document) {
        if (document == null || document.getId() == null) {
            return null;
        }
        return documents.findByIdWithMatchingContext(document.getId()).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private void applyNormalizedOcrFields(Document document, Map<String, Object> structuredData) {
        Map<String, Object> fields = structuredData.get("fields") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        document.setOcrDocumentType(asString(structuredData.get("documentType")));
        document.setOcrDetectedDocumentType(asString(structuredData.get("detectedDocumentType")));
        document.setOcrTypeConsistent(asBoolean(structuredData.get("typeConsistent")));
        document.setOcrLastName(asString(fields.get("lastName")));
        document.setOcrFirstName(asString(fields.get("firstName")));
        document.setOcrBirthDate(asString(fields.get("birthDate")));
        document.setOcrExpiryDate(asString(fields.get("expiryDate")));
        document.setOcrRegistrationNumber(asString(fields.get("registrationNumber")));
        document.setOcrBrand(asString(fields.get("brand")));
        document.setOcrModel(asString(fields.get("model")));
        document.setOcrEnergyType(asString(fields.get("energyType")));
        document.setOcrHolderName(asString(fields.get("holderName")));
        document.setOcrCompanyName(asString(fields.get("companyName")));
        document.setOcrSiren(asString(fields.get("siren")));
        document.setOcrInsuranceKind(asString(fields.get("insuranceKind")));
        document.setOcrIban(asString(fields.get("iban")));
        document.setOcrBic(asString(fields.get("bic")));
    }

    private String asString(Object value) {
        return Optional.ofNullable(value)
                .map(Object::toString)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .orElse(null);
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
