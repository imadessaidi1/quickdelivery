package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.entities.Document;

import java.util.Map;

public interface DocumentOcrService {
    boolean isEnabled();
    String providerName();
    OcrExtractionResult extract(Document document);

    record OcrExtractionResult(boolean success,
                               String rawText,
                               Map<String, Object> structuredData,
                               Double confidenceScore,
                               String errorCode) {
    }
}
