package com.quickdelivery.abstarct.helpers;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PDFGenerator {

    private static final DeviceRgb PRIMARY = new DeviceRgb(20, 28, 40);
    private static final DeviceRgb MUTED = new DeviceRgb(93, 106, 127);
    private static final DeviceRgb BORDER = new DeviceRgb(35, 35, 35);
    private static final DeviceRgb LIGHT = new DeviceRgb(247, 247, 247);

    public static void generatePdf(PackageDTO packageDTO, String qrFilePath, String filePath, Locale locale) throws FileNotFoundException, MalformedURLException {
        ResourceBundle bundle = ResourceBundle.getBundle("pdfLabels", locale);
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(14, 14, 14, 14);

        AddressDTO departureAddress = packageDTO.getAddresses().stream()
                .filter(addressDTO -> ADDRESS_TYPE.DEPARTURE.equals(addressDTO.getType()))
                .collect(Collectors.toList()).get(0);
        AddressDTO arrivalAddress = packageDTO.getAddresses().stream()
                .filter(addressDTO -> ADDRESS_TYPE.ARRIVAL.equals(addressDTO.getType()))
                .collect(Collectors.toList()).get(0);

        Div label = new Div()
                .setBorder(new SolidBorder(BORDER, 1.4f))
                .setPadding(10)
                .setBackgroundColor(new DeviceRgb(255, 255, 255));

        label.add(createTopBand(bundle));
        label.add(createRecipientSection(arrivalAddress, bundle));
        label.add(createInfoAndQrSection(packageDTO, departureAddress, bundle, qrFilePath, pdf));
        label.add(createReferenceBlock(packageDTO));
        label.add(createBarcodeSection(packageDTO, pdf));

        document.add(label);
        document.close();
    }

    private static Table createTopBand(ResourceBundle bundle) {
        Table top = new Table(UnitValue.createPercentArray(new float[]{58, 22, 20}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(6);

        top.addCell(noBorderCell()
                .add(new Paragraph("QuickDelivery")
                        .setFontSize(20)
                        .setBold()
                        .setFontColor(PRIMARY)
                        .setMargin(0))
                .add(new Paragraph(label(bundle, "package.title", "Shipping label"))
                        .setFontSize(8)
                        .setFontColor(MUTED)
                        .setMarginTop(2)
                        .setMarginBottom(0)));

        top.addCell(metricCell(label(bundle, "package.service", "Service"), "J+2"));
        top.addCell(metricCell(label(bundle, "package.domestic", "Zone"), "DOM"));
        return top;
    }

    private static Cell metricCell(String title, String value) {
        return new Cell()
                .setBorder(new SolidBorder(BORDER, 0.8f))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(title)
                        .setFontSize(7)
                        .setFontColor(MUTED)
                        .setMarginBottom(2)
                        .setMarginTop(0))
                .add(new Paragraph(value)
                        .setBold()
                        .setFontSize(16)
                        .setMargin(0));
    }

    private static Div createRecipientSection(AddressDTO recipient, ResourceBundle bundle) {
        Div section = new Div()
                .setBorder(new SolidBorder(BORDER, 1f))
                .setPadding(10)
                .setMarginBottom(6);

        section.add(new Paragraph(label(bundle, "package.receiver", "Recipient"))
                .setFontSize(8)
                .setBold()
                .setFontColor(MUTED)
                .setMarginTop(0)
                .setMarginBottom(6));

        section.add(new Paragraph(safeUpper(fullName(recipient)))
                .setFontSize(13)
                .setBold()
                .setMarginTop(0)
                .setMarginBottom(4));

        section.add(new Paragraph(compactAddress(recipient))
                .setFontSize(11)
                .setMargin(0)
                .setMultipliedLeading(1.05f));

        String cityLine = (safe(recipient.getZipCode()) + " " + safeUpper(recipient.getTown())).trim();
        if (!cityLine.isBlank()) {
            section.add(new Paragraph(cityLine)
                    .setFontSize(20)
                    .setBold()
                    .setMarginTop(8)
                    .setMarginBottom(0));
        }

        return section;
    }

    private static Table createInfoAndQrSection(PackageDTO packageDTO, AddressDTO departureAddress, ResourceBundle bundle, String qrFilePath, PdfDocument pdf) throws MalformedURLException {
        Table section = new Table(UnitValue.createPercentArray(new float[]{62, 38}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(6);

        Cell left = new Cell()
                .setBorder(new SolidBorder(BORDER, 1f))
                .setPadding(8);
        left.add(infoLine(label(bundle, "package.id", "Tracking number"), packageDTO.getReference(), true));
        left.add(infoLine(label(bundle, "package.date", "Created on"), formatDate(packageDTO), false));
        left.add(infoLine(label(bundle, "package.weightLabel", "Weight"),
                MessageFormat.format(label(bundle, "package.weightValue", "{0} kg"), packageDTO.getWeight()), false));
        left.add(infoLine(label(bundle, "package.dimensions", "Dimensions"),
                MessageFormat.format(label(bundle, "package.dimensionsValue", "{0} x {1} x {2} cm"),
                        packageDTO.getHeight(), packageDTO.getWidth(), packageDTO.getDepth()), false));
        left.add(new Paragraph(label(bundle, "package.sender", "Sender"))
                .setFontSize(8)
                .setBold()
                .setFontColor(MUTED)
                .setMarginTop(8)
                .setMarginBottom(4));
        left.add(new Paragraph(safe(fullName(departureAddress)) + "\n" + compactAddress(departureAddress))
                .setFontSize(9)
                .setMargin(0)
                .setMultipliedLeading(1.05f));

        Cell right = new Cell()
                .setBorder(new SolidBorder(BORDER, 1f))
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(new Paragraph(label(bundle, "package.scanZone", "Scan zone"))
                .setFontSize(8)
                .setBold()
                .setFontColor(MUTED)
                .setMarginTop(0)
                .setMarginBottom(6));
        right.add(new Image(ImageDataFactory.create(qrFilePath))
                .setWidth(116)
                .setHorizontalAlignment(HorizontalAlignment.CENTER));

        Barcode128 barcodePreview = new Barcode128(pdf);
        barcodePreview.setCode(packageDTO.getReference());
        right.add(new Paragraph(barcodePreview.getCode())
                .setFontSize(8)
                .setMarginTop(6)
                .setMarginBottom(0));

        section.addCell(left);
        section.addCell(right);
        return section;
    }

    private static Div createReferenceBlock(PackageDTO packageDTO) {
        Div block = new Div()
                .setBorder(new SolidBorder(BORDER, 1f))
                .setBackgroundColor(LIGHT)
                .setPadding(8)
                .setMarginBottom(6);
        block.add(new Paragraph(packageDTO.getReference())
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMargin(0));
        return block;
    }

    private static Div createBarcodeSection(PackageDTO packageDTO, PdfDocument pdf) {
        Barcode128 barcode = new Barcode128(pdf);
        barcode.setCodeType(Barcode128.CODE128);
        barcode.setCode(packageDTO.getReference());
        barcode.setFont(null);
        barcode.setBarHeight(54);
        barcode.setX(1.2f);

        Image barcodeImage = new Image(barcode.createFormXObject(pdf))
                .setWidth(UnitValue.createPercentValue(100))
                .setAutoScaleHeight(true);

        Div section = new Div()
                .setBorder(new SolidBorder(BORDER, 1f))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER);
        section.add(barcodeImage);
        section.add(new Paragraph(packageDTO.getReference())
                .setFontSize(9)
                .setMarginTop(6)
                .setMarginBottom(0));
        return section;
    }

    private static Paragraph infoLine(String title, String value, boolean emphasized) {
        Paragraph paragraph = new Paragraph()
                .setMarginTop(0)
                .setMarginBottom(4);
        paragraph.add(new Text(title + ": ").setFontSize(8).setFontColor(MUTED).setBold());
        Text valueText = new Text(safe(value)).setFontSize(emphasized ? 11 : 9);
        if (emphasized) {
            valueText.setBold();
        }
        paragraph.add(valueText);
        return paragraph;
    }

    private static Cell noBorderCell() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    private static String fullName(AddressDTO address) {
        return (safe(address.getFirstName()) + " " + safe(address.getLastName())).trim();
    }

    private static String compactAddress(AddressDTO address) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, address.getLine1());
        appendLine(builder, address.getLine2());
        if (!safe(address.getCountry()).isBlank()) {
            appendLine(builder, address.getCountry());
        }
        if (!safe(address.getPhone()).isBlank()) {
            appendLine(builder, address.getPhone());
        }
        return builder.toString().trim();
    }

    private static void appendLine(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
        builder.append(value.trim());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeUpper(String value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }

    private static String formatDate(PackageDTO packageDTO) {
        if (packageDTO.getCreationDate() == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return packageDTO.getCreationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .format(formatter);
    }

    private static String label(ResourceBundle bundle, String key, String fallback) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return fallback;
        }
    }
}
