package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_OCR_STATUS;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.services.interfaces.DocumentOcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class DocumentOcrOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(DocumentOcrOrchestrator.class);
    private final Documents documents;
    private final DocumentOcrService documentOcrService;
    private final ObjectMapper objectMapper;
    private final DocumentProfileMatchingService documentProfileMatchingService;

    public DocumentOcrOrchestrator(Documents documents,
                                   DocumentOcrService documentOcrService,
                                   ObjectMapper objectMapper,
                                   DocumentProfileMatchingService documentProfileMatchingService) {
        this.documents = documents;
        this.documentOcrService = documentOcrService;
        this.objectMapper = objectMapper;
        this.documentProfileMatchingService = documentProfileMatchingService;
    }

    @Async("ocrTaskExecutor")
    @Transactional
    public void processDocumentAsync(Long documentId) {
        logger.info("Starting OCR processing for document {}", documentId);
        Document document = documents.findById(documentId).orElse(null);
        if (document == null) {
            logger.warn("OCR document {} not found", documentId);
            return;
        }

        if (document.getDocURL() == null || document.getDocURL().isBlank()) {
            markDisabled(documentId, "NO_DOCUMENT_PATH");
            return;
        }

        if (!documentOcrService.isEnabled()) {
            markDisabled(documentId, "OCR_DISABLED");
            return;
        }

        if (!waitForDocumentFile(document.getDocURL())) {
            markFailure(documentId, "FILE_NOT_READY");
            return;
        }

        try {
            updateDocument(documentId, currentDocument -> {
                currentDocument.setOcrStatus(DOCUMENT_OCR_STATUS.PROCESSING);
                currentDocument.setOcrProvider(documentOcrService.providerName());
                currentDocument.setOcrErrorCode(null);
            });

            DocumentOcrService.OcrExtractionResult extractionResult = documentOcrService.extract(document);
            if (!extractionResult.success()) {
                if (extractionResult.errorCode() != null
                        && (extractionResult.errorCode().startsWith("OCR_NOT_APPLICABLE")
                        || extractionResult.errorCode().startsWith("OCR_MANUAL_REVIEW"))) {
                    markDisabled(documentId, extractionResult.errorCode());
                    return;
                }
                markFailure(documentId, extractionResult.errorCode());
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
            updateDocument(documentId, currentDocument -> {
                currentDocument.setOcrStatus(DOCUMENT_OCR_STATUS.COMPLETED);
                currentDocument.setOcrProvider(documentOcrService.providerName());
                currentDocument.setOcrConfidenceScore(extractionResult.confidenceScore());
                currentDocument.setOcrExtractedData(extractedData);
                currentDocument.setOcrProcessedAt(new Date());
                currentDocument.setOcrErrorCode(extractionResult.errorCode());
                currentDocument.setMatchStatus(matchingResult.status());
                currentDocument.setMatchScore(matchingResult.score());
                currentDocument.setMatchDetails(matchDetails);
            });
            logger.info("OCR processing completed for document {} with provider {}", documentId, documentOcrService.providerName());
        } catch (Exception exception) {
            logger.warn("OCR processing failed for document {}: {}", documentId, exception.getMessage(), exception);
            markFailure(documentId, "OCR_PROCESSING_ERROR");
        }
    }

    private boolean waitForDocumentFile(String pathValue) {
        Path path = Path.of(pathValue);
        for (int attempt = 0; attempt < 10; attempt++) {
            if (Files.exists(path)) {
                return true;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void markDisabled(Long documentId, String errorCode) {
        updateDocument(documentId, document -> {
            document.setOcrStatus(DOCUMENT_OCR_STATUS.DISABLED);
            document.setOcrProvider(documentOcrService.providerName());
            document.setOcrErrorCode(errorCode);
            document.setOcrProcessedAt(new Date());
            document.setMatchStatus(errorCode.startsWith("OCR_MANUAL_REVIEW") ? DOCUMENT_MATCH_STATUS.REVIEW_REQUIRED : DOCUMENT_MATCH_STATUS.NOT_APPLICABLE);
            document.setMatchScore(null);
            document.setMatchDetails(null);
        });
        logger.info("OCR disabled for document {} with code {}", documentId, errorCode);
    }

    private void markFailure(Long documentId, String errorCode) {
        updateDocument(documentId, document -> {
            document.setOcrStatus(DOCUMENT_OCR_STATUS.FAILED);
            document.setOcrProvider(documentOcrService.providerName());
            document.setOcrErrorCode(errorCode);
            document.setOcrProcessedAt(new Date());
            document.setMatchStatus(DOCUMENT_MATCH_STATUS.UNAVAILABLE);
            document.setMatchScore(null);
            document.setMatchDetails(null);
        });
        logger.warn("OCR failed for document {} with code {}", documentId, errorCode);
    }

    private void updateDocument(Long documentId, Consumer<Document> updater) {
        Document currentDocument = documents.findById(documentId).orElse(null);
        if (currentDocument == null) {
            logger.warn("OCR update skipped because document {} no longer exists", documentId);
            return;
        }
        updater.accept(currentDocument);
        documents.save(currentDocument);
    }
}
