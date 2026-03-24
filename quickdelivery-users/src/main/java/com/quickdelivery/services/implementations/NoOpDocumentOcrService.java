package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.services.interfaces.DocumentOcrService;
import org.springframework.stereotype.Service;

@Service
public class NoOpDocumentOcrService implements DocumentOcrService {
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String providerName() {
        return "NONE";
    }

    @Override
    public OcrExtractionResult extract(Document document) {
        return new OcrExtractionResult(false, null, null, null, "OCR_DISABLED");
    }
}
