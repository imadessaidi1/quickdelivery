package com.quickdelivery.abstarct.helpers;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
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
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PDFGenerator {
    public static void generatePdf(PackageDTO packageDTO, String qrFilePath, String filePath, Locale locale) throws FileNotFoundException, MalformedURLException {
        ResourceBundle bundle = ResourceBundle.getBundle("pdfLabels", locale);
        // Create a PdfWriter object
        PdfWriter writer = new PdfWriter(filePath);

        // Create a PdfDocument object
        PdfDocument pdf = new PdfDocument(writer);

        // Create a Document object
        Document document = new Document(pdf);

        // Add a title to the document
        document.add(new Paragraph(bundle.getString("package.title"))
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16)
                .setMarginBottom(10));

        // Add package details
        document.add(new Paragraph(bundle.getString("package.id") + packageDTO.getReference())
                .setMarginBottom(5));
        document.add(new Paragraph(bundle.getString("package.date") + packageDTO.getCreationDate())
                .setMarginBottom(5));
        document.add(new Paragraph(MessageFormat.format(bundle.getString("package.weight"), packageDTO.getWeight()))
                .setMarginBottom(15));

        // Add sender and receiver details in a table
        Table addressTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        AddressDTO departureAddress = packageDTO.getAddresses().stream()
                .filter(addressDTO -> addressDTO.getType().equals(ADDRESS_TYPE.DEPARTURE))
                .collect(Collectors.toList()).get(0);
        AddressDTO arrivalAddress = packageDTO.getAddresses().stream()
                .filter(addressDTO -> addressDTO.getType().equals(ADDRESS_TYPE.ARRIVAL))
                .collect(Collectors.toList()).get(0);
        addressTable.addCell(createCell(bundle.getString("package.sender"), departureAddress.formatedtoString()+"\n"+departureAddress.getPhone()));
        addressTable.addCell(createCell(bundle.getString("package.receiver"), arrivalAddress.formatedtoString()+"\n"+arrivalAddress.getPhone()));
        document.add(addressTable);

        // Add QR code
        Image qrCodeImage = new Image(ImageDataFactory.create(qrFilePath))
                .setWidth(100)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
        document.add(qrCodeImage);

        // Add instructions
        document.add(new Paragraph(bundle.getString("package.instruction")+ "________")
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(15));

        // Close the document
        document.close();
    }

    // Helper method to create a cell with specific alignment and background color
    private static Cell createCell(String title, String content) {
        Cell cell = new Cell()
                .add(new Paragraph(title + ": " + content))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(new DeviceRgb(240, 240, 240));
        return cell;
    }
}
