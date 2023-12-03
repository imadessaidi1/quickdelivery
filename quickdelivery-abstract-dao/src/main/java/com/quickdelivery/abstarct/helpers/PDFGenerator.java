package com.quickdelivery.abstarct.helpers;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.stream.Collectors;

public class PDFGenerator {
    public static void generatePdf(PackageDTO packageDTO, String qrfilePath, String filePath) throws FileNotFoundException, MalformedURLException {
        // Create a PdfWriter object
        PdfWriter writer = new PdfWriter(filePath);

        // Create a PdfDocument object
        PdfDocument pdf = new PdfDocument(writer);

        // Create a Document object
        Document document = new Document(pdf);

        // Add a title to the document
        document.add(new Paragraph(new Text("Colis : "+packageDTO.getId()+" / "+packageDTO.getCreationDate()+" / "+packageDTO.getAddresses().get(0).getCountry())));

        // Add a table to the document
        Table table = new Table(2);
        table.addCell(new Cell().add(new Paragraph(new Text(packageDTO.getAddresses().stream().filter(addressDTO -> addressDTO.getType().equals(ADDRESS_TYPE.DEPARTURE)).collect(Collectors.toList()).get(0).formatedtoString()))));
        table.addCell(new Cell().add(new Image(ImageDataFactory.create(qrfilePath))));
        table.addCell(new Cell().add(new Paragraph(new Text(packageDTO.getAddresses().stream().filter(addressDTO -> addressDTO.getType().equals(ADDRESS_TYPE.ARRIVAL)).collect(Collectors.toList()).get(0).formatedtoString()))));
        table.addCell(new Cell().add(new Paragraph(new Text("Comment"))));

        document.add(table);

        // Close the document
        document.close();
    }
}
