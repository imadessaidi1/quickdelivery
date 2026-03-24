package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.services.interfaces.DocumentOcrService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Primary
@ConditionalOnBean(TextractClient.class)
@ConditionalOnProperty(name = "quickdelivery.ocr.textract.enabled", havingValue = "true")
public class TextractDocumentOcrService implements DocumentOcrService {
    private final TextractClient textractClient;
    private final DocumentOcrUnderstandingService documentOcrUnderstandingService;

    public TextractDocumentOcrService(TextractClient textractClient,
                                      DocumentOcrUnderstandingService documentOcrUnderstandingService) {
        this.textractClient = textractClient;
        this.documentOcrUnderstandingService = documentOcrUnderstandingService;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String providerName() {
        return "AWS_TEXTRACT";
    }

    @Override
    public OcrExtractionResult extract(Document document) {
        try {
            if (document.getType() == DOCUMENT_TYPE.PICTURE) {
                return new OcrExtractionResult(false, null, null, null, "OCR_NOT_APPLICABLE_PICTURE");
            }
            Path path = Path.of(document.getDocURL());
            byte[] bytes = Files.readAllBytes(path);
            OcrPageResult pageResult;
            if (isPdf(path)) {
                try {
                    pageResult = extractPdfText(bytes);
                } catch (Exception pdfException) {
                    return new OcrExtractionResult(false, null, null, null, "OCR_MANUAL_REVIEW_PDF_UNREADABLE");
                }
            } else {
                pageResult = extractTextFromBytes(bytes);
            }

            Map<String, Object> structuredData = documentOcrUnderstandingService.understand(document.getType(), pageResult.text());
            Object typeConsistent = structuredData.get("typeConsistent");
            if (Boolean.FALSE.equals(typeConsistent)) {
                Object detectedType = structuredData.get("detectedDocumentType");
                return new OcrExtractionResult(true, pageResult.text(), structuredData, pageResult.confidence(),
                        "OCR_TYPE_MISMATCH_" + (detectedType == null ? "UNKNOWN" : detectedType.toString()));
            }
            return new OcrExtractionResult(true, pageResult.text(), structuredData, pageResult.confidence(), null);
        } catch (Exception exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }
            message = message.replaceAll("[\\r\\n]+", " ").trim();
            if (message.length() > 180) {
                message = message.substring(0, 180);
            }
            return new OcrExtractionResult(false, null, null, null, "TEXTRACT_ERROR: " + message);
        }
    }

    private OcrPageResult extractPdfText(byte[] pdfBytes) throws Exception {
        try (PDDocument pdfDocument = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            StringBuilder extractedText = new StringBuilder();
            double confidenceSum = 0d;
            int confidenceCount = 0;

            for (int pageIndex = 0; pageIndex < pdfDocument.getNumberOfPages(); pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    ImageIO.write(pageImage, "jpg", outputStream);
                    OcrPageResult pageResult = extractTextFromBytes(outputStream.toByteArray());
                    if (!pageResult.text().isBlank()) {
                        if (extractedText.length() > 0) {
                            extractedText.append('\n');
                        }
                        extractedText.append(pageResult.text());
                    }
                    if (pageResult.confidence() != null) {
                        confidenceSum += pageResult.confidence();
                        confidenceCount++;
                    }
                }
            }

            return new OcrPageResult(
                    extractedText.toString(),
                    confidenceCount == 0 ? 0d : confidenceSum / confidenceCount
            );
        }
    }

    private OcrPageResult extractTextFromBytes(byte[] bytes) {
        var response = textractClient.detectDocumentText(DetectDocumentTextRequest.builder()
                .document(software.amazon.awssdk.services.textract.model.Document.builder()
                        .bytes(SdkBytes.fromByteArray(bytes))
                        .build())
                .build());

        List<Block> lineBlocks = response.blocks().stream()
                .filter(block -> "LINE".equals(block.blockTypeAsString()))
                .sorted(Comparator.comparing(Block::id, Comparator.nullsLast(String::compareTo)))
                .toList();

        String extractedText = lineBlocks.stream()
                .map(Block::text)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        Double averageConfidence = lineBlocks.stream()
                .map(Block::confidence)
                .filter(confidence -> confidence != null)
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0d);

        return new OcrPageResult(extractedText, averageConfidence);
    }

    private boolean isPdf(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".pdf");
    }

    private record OcrPageResult(String text, Double confidence) {
    }
}
