package com.rifas.publicas.sorteo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.sorteo.model.BoletoDigital;
import com.rifas.publicas.sorteo.repository.BoletoDigitalRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import com.lowagie.text.*;
import java.awt.Color;

@Service
public class BoletoDigitalService {

    private final BoletoDigitalRepository boletoDigitalRepository;

    private final BoletoRepository boletoRepository;

    private final CryptoService cryptoService;

    BoletoDigitalService(BoletoDigitalRepository boletoDigitalRepository, BoletoRepository boletoRepository,
            CryptoService cryptoService) {
        this.boletoDigitalRepository = boletoDigitalRepository;
        this.boletoRepository = boletoRepository;
        this.cryptoService = cryptoService;
    }

    public String generarQrBoletoDigitalBase64(Long boletoId) {
        BoletoDigital boletoDigital = obtenerOCrearBoletoDigital(boletoId);
        String contenidoQr = construirContenidoQr(boletoDigital);

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contenidoQr, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar QR", e);
        }
    }

    /**
     * Genera un archivo PDF dinámico consultando los datos de la rifa por su ID, marco elegante, QR intacto y sello seguro al pie.
     */
    public byte[] generarPdfBoleto(Long boletoId) {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
        log.info("Iniciando generación de PDF dinámico para el boleto ID: {}", boletoId);

        Boleto boleto = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
        
        // Obtenemos los datos de la rifa asociada al boleto (ajusta el método según tu modelo, ej: boleto.getRifa())
        var rifa = boleto.getRifa(); 

        BoletoDigital boletoDigital = obtenerOCrearBoletoDigital(boletoId);
        String contenidoQr = construirContenidoQr(boletoDigital);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 35, 35);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            // --- MARCO EXTERIOR ELEGANTE ---
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContent();
                    canvas.saveState();
                    
                    float pageWidth = document.getPageSize().getWidth();
                    float pageHeight = document.getPageSize().getHeight();
                    
                    canvas.setColorStroke(new Color(71, 85, 105)); 
                    canvas.setLineWidth(1.2f);
                    canvas.rectangle(18, 18, pageWidth - 36, pageHeight - 36);
                    canvas.stroke();
                    
                    canvas.setColorStroke(new Color(199, 210, 254)); 
                    canvas.setLineWidth(0.5f);
                    canvas.rectangle(22, 22, pageWidth - 44, pageHeight - 44);
                    canvas.stroke();
                    
                    canvas.restoreState();
                }
            });

            document.open();

            // Colores profesionales
            Color primaryColor = new Color(30, 41, 59);   
            Color accentColor = new Color(79, 70, 229);   
            Color cardBgColor = new Color(248, 250, 252); 
            Color borderColor = new Color(203, 213, 225); 

            // Fuentes estilizadas
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, primaryColor);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, primaryColor);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            // --- ENCABEZADO ---
            Paragraph title = new Paragraph("BOLETO DIGITAL OFICIAL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subTitle = new Paragraph("Sorteo / Rifa Autorizada", subTitleFont);
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(10);
            document.add(subTitle);

            // --- LÍNEA DIVISORA CENTRAL ---
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(40);
            lineTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(PdfPCell.BOTTOM);
            lineCell.setBorderColor(accentColor);
            lineCell.setBorderWidth(1f);
            lineCell.setFixedHeight(4f);
            lineTable.addCell(lineCell);
            lineTable.setSpacingAfter(12);
            document.add(lineTable);

            // --- TARJETA CONTENEDORA CON DATOS DINÁMICOS DE LA RIFA ---
            PdfPTable cardContainer = new PdfPTable(1);
            cardContainer.setWidthPercentage(70); // Ancho ideal para descripciones dinámicas
            cardContainer.setHorizontalAlignment(Element.ALIGN_CENTER);
            cardContainer.setSpacingAfter(15);

            PdfPCell cardCell = new PdfPCell();
            cardCell.setBackgroundColor(cardBgColor);
            cardCell.setBorder(PdfPCell.BOX);
            cardCell.setBorderColor(borderColor);
            cardCell.setBorderWidth(1f);
            cardCell.setPadding(12); 

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[] { 38f, 62f });

            // Inyección de datos reales desde la base de datos
            detailsTable.addCell(createCell("Participa por:", labelFont, PdfPCell.NO_BORDER, Element.ALIGN_RIGHT));
            detailsTable.addCell(createCell(" " + rifa.getTitulo(), valueFont, PdfPCell.NO_BORDER, Element.ALIGN_LEFT));

            detailsTable.addCell(createCell("Número de Boleto:", labelFont, PdfPCell.NO_BORDER, Element.ALIGN_RIGHT));
            detailsTable.addCell(createCell(" #" + boleto.getNumeroBoleto(), valueFont, PdfPCell.NO_BORDER, Element.ALIGN_LEFT));

            detailsTable.addCell(createCell("ID de Registro:", labelFont, PdfPCell.NO_BORDER, Element.ALIGN_RIGHT));
            detailsTable.addCell(createCell(" " + boleto.getId(), valueFont, PdfPCell.NO_BORDER, Element.ALIGN_LEFT));

            // Formateo seguro de fecha y precio
            String fechaSorteoStr = rifa.getFechaSorteo() != null ? rifa.getFechaSorteo().toString() : "Por definir";
            detailsTable.addCell(createCell("Fecha del Sorteo:", labelFont, PdfPCell.NO_BORDER, Element.ALIGN_RIGHT));
            detailsTable.addCell(createCell(" " + fechaSorteoStr, valueFont, PdfPCell.NO_BORDER, Element.ALIGN_LEFT));

            String precioStr = rifa.getPrecioBoleto() != null ? "$" + rifa.getPrecioBoleto() + " MXN" : "N/D";
            detailsTable.addCell(createCell("Precio del Boleto:", labelFont, PdfPCell.NO_BORDER, Element.ALIGN_RIGHT));
            detailsTable.addCell(createCell(" " + precioStr, valueFont, PdfPCell.NO_BORDER, Element.ALIGN_LEFT));

            cardCell.addElement(detailsTable);
            cardContainer.addCell(cardCell);
            document.add(cardContainer);

            // --- CÓDIGO QR (Intacto y centrado) ---
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(contenidoQr, BarcodeFormat.QR_CODE, 220, 220);
            ByteArrayOutputStream qrOs = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOs);

            Image qrImage = Image.getInstance(qrOs.toByteArray());
            qrImage.scaleToFit(140, 140); 
            qrImage.setAlignment(Image.ALIGN_CENTER);
            
            Paragraph qrWrapper = new Paragraph();
            qrWrapper.setSpacingBefore(0);
            qrWrapper.setSpacingAfter(15);
            qrWrapper.add(qrImage);
            document.add(qrWrapper);

            // --- LÍNEA DIVISORA DE SEGURIDAD PARA EL PIE ---
            PdfPTable footerLineTable = new PdfPTable(1);
            footerLineTable.setWidthPercentage(80);
            footerLineTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell footerLineCell = new PdfPCell();
            footerLineCell.setBorder(PdfPCell.TOP);
            footerLineCell.setBorderColor(new Color(226, 232, 240));
            footerLineCell.setBorderWidth(0.5f);
            footerLineCell.setFixedHeight(2f);
            footerLineTable.addCell(footerLineCell);
            footerLineTable.setSpacingAfter(8);
            document.add(footerLineTable);

            // --- SELLO CRIPTOGRÁFICO AL PIE DE PÁGINA ---
            Paragraph footerSello = new Paragraph(
                    "Sello Criptográfico (HMAC): " + boletoDigital.getSelloDigital(), 
                    footerFont
            );
            footerSello.setAlignment(Element.ALIGN_CENTER);
            document.add(footerSello);

            document.close();
            log.info("PDF dinámico generado con éxito para el boleto ID: {}", boletoId);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar el PDF dinámico del boleto: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF del boleto", e);
        }
    }

    private PdfPCell createCell(String text, Font font, int border, int alignment) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(border);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(3);
        return cell;
    }

    // public byte[] generarPdfBoleto(Long boletoId) {
    // Boleto boleto = boletoRepository.findById(boletoId)
    // .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
    // BoletoDigital boletoDigital = obtenerOCrearBoletoDigital(boletoId);
    // String contenidoQr = construirContenidoQr(boletoDigital);

    // try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
    // Document document = new Document();
    // PdfWriter.getInstance(document, baos);
    // document.open();

    // document.add(new Paragraph("=== BOLETO DIGITAL OFICIAL ==="));
    // document.add(new Paragraph("Rifa / Sorteo Público"));
    // document.add(new Paragraph(" "));
    // document.add(new Paragraph("Número de Boleto: " + boleto.getNumeroBoleto()));
    // document.add(new Paragraph("ID de Registro: " + boleto.getId()));
    // document.add(new Paragraph("Sello Criptográfico (HMAC): " +
    // boletoDigital.getSelloDigital()));
    // document.add(new Paragraph(" "));

    // // Generar imagen QR en bytes para el PDF
    // QRCodeWriter qrCodeWriter = new QRCodeWriter();
    // BitMatrix bitMatrix = qrCodeWriter.encode(contenidoQr, BarcodeFormat.QR_CODE,
    // 150, 150);
    // ByteArrayOutputStream qrOs = new ByteArrayOutputStream();
    // MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOs);

    // Image qrImage = Image.getInstance(qrOs.toByteArray());
    // qrImage.setAlignment(Image.ALIGN_CENTER);
    // document.add(qrImage);

    // document.close();
    // return baos.toByteArray();
    // } catch (Exception e) {
    // throw new RuntimeException("Error al generar el PDF del boleto", e);
    // }
    // }


    public BoletoDigital obtenerOCrearBoletoDigital(Long boletoId) {
        return boletoDigitalRepository.findByBoletoId(boletoId).orElseGet(() -> {
            Boleto boleto = boletoRepository.findById(boletoId)
                    .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
            String randomState = cryptoService.generarRandomState();
            String sello = cryptoService.generarSelloDigital(boleto.getId(), String.valueOf(boleto.getNumeroBoleto()),
                    "cliente@rifas.com", randomState);

            BoletoDigital nuevo = new BoletoDigital();
            nuevo.setBoleto(boleto); // Le pasas el objeto Boleto completo
            nuevo.setRandomState(randomState);
            nuevo.setSelloDigital(sello);
            return boletoDigitalRepository.save(nuevo);
        });
    }

    private String construirContenidoQr(BoletoDigital bd) {
        return "http://localhost:8080/api/sorteo/verificar?id=" + bd.getBoleto().getId()
                + "&rs=" + bd.getRandomState()
                + "&sello=" + bd.getSelloDigital();
    }
}