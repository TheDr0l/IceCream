package com.example.IceCream_SpringBoot.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class GenerateFacturaPDF {

    public byte[] generarFacturaPDF(
            String nombreCliente,
            List<String> nombresHelados,
            List<Integer> unidadesVenderLista,
            String email,
            String telefono,
            String metodoPago,
            double total) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // --- LOGOS ---
            Image logo = cargarImagen("/static/img/heladoCool.png", 80, 80);
            Image titulo = cargarImagen("/static/img/titulo.png", 200, 70);

            // Cabecera con logo + titulo
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[] { 1, 2 });

            PdfPCell cellLogo = new PdfPCell(logo, false);
            cellLogo.setBorder(Rectangle.NO_BORDER);

            PdfPCell cellTitulo = new PdfPCell(titulo, false);
            cellTitulo.setBorder(Rectangle.NO_BORDER);
            cellTitulo.setHorizontalAlignment(Element.ALIGN_RIGHT);

            headerTable.addCell(cellLogo);
            headerTable.addCell(cellTitulo);
            document.add(headerTable);

            document.add(Chunk.NEWLINE);

            // --- TiTULO PRINCIPAL ---
            Paragraph tituloFactura = new Paragraph(
                    "FACTURA DE COMPRA",
                    new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(0, 102, 204)));
            tituloFactura.setAlignment(Element.ALIGN_CENTER);
            document.add(tituloFactura);

            document.add(Chunk.NEWLINE);

            // --- DATOS DEL CLIENTE ---
            Font fontNormal = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);

            PdfPTable tablaDatos = new PdfPTable(2);
            tablaDatos.setWidthPercentage(80);
            tablaDatos.setHorizontalAlignment(Element.ALIGN_CENTER);
            tablaDatos.setSpacingBefore(10);

            tablaDatos.addCell(celda("Cliente:", fontNormal, true));
            tablaDatos.addCell(celda(nombreCliente, fontNormal, false));

            tablaDatos.addCell(celda("Telefono:", fontNormal, true));
            tablaDatos.addCell(celda(telefono, fontNormal, false));

            tablaDatos.addCell(celda("Email:", fontNormal, true));
            tablaDatos.addCell(celda(email, fontNormal, false));

            tablaDatos.addCell(celda("Metodo de pago:", fontNormal, true));
            tablaDatos.addCell(celda(metodoPago, fontNormal, false));

            document.add(tablaDatos);

            document.add(Chunk.NEWLINE);

            // --- TABLA DE HELADOS ---
            PdfPTable tablaHelados = new PdfPTable(2);
            tablaHelados.setWidthPercentage(80);
            tablaHelados.setHorizontalAlignment(Element.ALIGN_CENTER);
            tablaHelados.setSpacingBefore(15);

            Font fontHeader = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.WHITE);
            PdfPCell header1 = new PdfPCell(new Phrase("Helado", fontHeader));
            PdfPCell header2 = new PdfPCell(new Phrase("Cantidad", fontHeader));

            header1.setBackgroundColor(new BaseColor(0, 102, 204));
            header2.setBackgroundColor(new BaseColor(0, 102, 204));
            header1.setHorizontalAlignment(Element.ALIGN_CENTER);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);

            tablaHelados.addCell(header1);
            tablaHelados.addCell(header2);

            for (int i = 0; i < nombresHelados.size(); i++) {
                tablaHelados.addCell(celda(nombresHelados.get(i), fontNormal, false));
                tablaHelados.addCell(celda(String.valueOf(unidadesVenderLista.get(i)), fontNormal, false));
            }

            document.add(tablaHelados);

            document.add(Chunk.NEWLINE);

            // --- TOTAL ---
            Paragraph totalTxt = new Paragraph(
                    "Total a pagar: $" + String.format("%,.2f", total) + " COP",
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 102, 204)));
            totalTxt.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalTxt);

            document.add(Chunk.NEWLINE);

            // --- MENSAJE FINAL ---
            Paragraph gracias = new Paragraph(
                    "¡Gracias por su compra en Ice Cream!",
                    new Font(Font.FontFamily.HELVETICA, 13, Font.BOLDITALIC, new BaseColor(0, 102, 204)));
            gracias.setAlignment(Element.ALIGN_CENTER);
            document.add(gracias);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0]; 
        }
    }

    // Metodo auxiliar para crear celdas
    private PdfPCell celda(String texto, Font fuente, boolean esTitulo) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(esTitulo ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        cell.setPadding(5);
        return cell;
    }

    // Metodo auxiliar para cargar imagenes de recursos
    private Image cargarImagen(String ruta, float ancho, float alto) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(ruta)) {
            if (is == null)
                throw new Exception("No se encontro la imagen: " + ruta);
            Image img = Image.getInstance(is.readAllBytes());
            img.scaleToFit(ancho, alto);
            return img;
        }
    }
}
