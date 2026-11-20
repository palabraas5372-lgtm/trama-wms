package com.trama.wms.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.trama.wms.model.DetalleVenta;
import com.trama.wms.model.Usuario;
import com.trama.wms.model.Venta;
import com.trama.wms.repository.VentaRepository;

/** Genera la boleta o factura electrónica de una venta en PDF (librería OpenPDF). */
@Service
public class ComprobantePdfService {

    private static final Color DENIM = new Color(0x22, 0x35, 0x54);
    private static final Color HILO = new Color(0xE8, 0xA3, 0x3D);
    private static final Color TELA = new Color(0xF4, 0xF2, 0xEC);
    private static final Color GRIS = new Color(0x6B, 0x72, 0x80);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaRepository ventaRepository;

    public ComprobantePdfService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Transactional(readOnly = true)
    public Venta buscar(Integer idVenta) {
        Venta v = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new PedidoInvalidoException("Comprobante no encontrado."));
        // Inicializa las relaciones perezosas dentro de la transacción
        v.getDetalles().forEach(d -> d.getProducto().getNombre());
        v.getCliente().getNombreCompleto();
        if (v.getSucursal() != null) v.getSucursal().getNombre();
        v.getPedido().getCodigo();
        return v;
    }

    @Transactional(readOnly = true)
    public byte[] generar(Integer idVenta) {
        Venta v = buscar(idVenta);
        Usuario c = v.getCliente();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, DENIM);
        Font marca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, DENIM);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        Font chico = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
        Font blanco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font totalF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DENIM);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ── Cabecera: empresa a la izquierda, recuadro del comprobante a la derecha ──
        PdfPTable cab = new PdfPTable(new float[] { 60, 40 });
        cab.setWidthPercentage(100);

        PdfPCell empresa = celda("", normal, Element.ALIGN_LEFT);
        empresa.addElement(new Paragraph("TRAMA S.A.", marca));
        empresa.addElement(new Paragraph("Distribución textil · Almacén Central", chico));
        empresa.addElement(new Paragraph("RUC 20601234567", normal));
        if (v.getSucursal() != null) {
            empresa.addElement(new Paragraph("Atendido por: " + v.getSucursal().getNombre()
                    + (v.getSucursal().getTelefono() != null ? " · Tel. " + v.getSucursal().getTelefono() : ""), chico));
        }
        cab.addCell(empresa);

        PdfPCell caja = new PdfPCell();
        caja.setBorderColor(DENIM);
        caja.setBorderWidth(1.5f);
        caja.setPadding(10);
        Paragraph pRuc = new Paragraph("RUC 20601234567", negrita);
        pRuc.setAlignment(Element.ALIGN_CENTER);
        Paragraph pTipo = new Paragraph((v.esFactura() ? "FACTURA" : "BOLETA DE VENTA") + " ELECTRÓNICA", titulo);
        pTipo.setAlignment(Element.ALIGN_CENTER);
        Paragraph pNum = new Paragraph(v.getComprobante(), negrita);
        pNum.setAlignment(Element.ALIGN_CENTER);
        caja.addElement(pRuc);
        caja.addElement(pTipo);
        caja.addElement(pNum);
        cab.addCell(caja);
        doc.add(cab);

        // ── Datos del cliente y de la venta ──
        PdfPTable datos = new PdfPTable(new float[] { 22, 38, 18, 22 });
        datos.setWidthPercentage(100);
        datos.setSpacingBefore(16);
        if (v.esFactura()) {
            fila(datos, "Razón social:", v.getRazonSocial(), negrita, normal);
            fila(datos, "RUC:", v.getRuc(), negrita, normal);
        } else {
            fila(datos, "Cliente:", c.getNombreCompleto(), negrita, normal);
            fila(datos, "DNI:", c.getDni() == null ? "—" : c.getDni(), negrita, normal);
        }
        fila(datos, "Fecha de emisión:", v.getFecha().format(FECHA), negrita, normal);
        fila(datos, "Pedido:", v.getPedido().getCodigo(), negrita, normal);
        fila(datos, "Método de pago:", VentaService.METODOS_PAGO.getOrDefault(v.getMetodoPago(), v.getMetodoPago()), negrita, normal);
        fila(datos, "Moneda:", "Soles (PEN)", negrita, normal);
        doc.add(datos);

        // ── Detalle de productos ──
        PdfPTable det = new PdfPTable(new float[] { 12, 46, 12, 15, 15 });
        det.setWidthPercentage(100);
        det.setSpacingBefore(16);
        det.setHeaderRows(1);
        for (String h : new String[] { "Cant.", "Descripción", "Código", "P. unit.", "Importe" }) {
            PdfPCell hc = celda(h, blanco, Element.ALIGN_CENTER);
            hc.setBackgroundColor(DENIM);
            if (h.equals("Descripción")) hc.setHorizontalAlignment(Element.ALIGN_LEFT);
            det.addCell(hc);
        }
        boolean par = false;
        for (DetalleVenta d : v.getDetalles()) {
            Color fondo = par ? TELA : Color.WHITE;
            par = !par;
            String desc = d.getProducto().getNombre()
                    + (d.getProducto().getTalla() != null ? " · Talla " + d.getProducto().getTalla() : "")
                    + (d.getProducto().getColor() != null ? " · " + d.getProducto().getColor() : "");
            det.addCell(fondo(celda(String.valueOf(d.getCantidad()), normal, Element.ALIGN_CENTER), fondo));
            det.addCell(fondo(celda(desc, normal, Element.ALIGN_LEFT), fondo));
            det.addCell(fondo(celda(d.getProducto().getCodigo(), normal, Element.ALIGN_CENTER), fondo));
            det.addCell(fondo(celda(soles(d.getPrecioUnitario()), normal, Element.ALIGN_RIGHT), fondo));
            det.addCell(fondo(celda(soles(d.getImporte()), normal, Element.ALIGN_RIGHT), fondo));
        }
        doc.add(det);

        // ── Totales ──
        PdfPTable tot = new PdfPTable(new float[] { 60, 25, 15 });
        tot.setWidthPercentage(100);
        tot.setSpacingBefore(10);
        total(tot, "Subtotal (" + v.getTotalUnidades() + " unidades)", soles(v.getSubtotal()), normal);
        if (v.getVolumenPct() > 0) {
            total(tot, "Descuento por volumen (" + v.getVolumenPct() + "%)", "- " + soles(v.getDescuentoVolumen()), normal);
        }
        if (v.getFestivoPctSeguro() > 0) {
            total(tot, "Descuento " + v.getFestivoNombre() + " (" + v.getFestivoPctSeguro() + "%)",
                    "- " + soles(v.getDescuentoFestivo()), normal);
        }
        if (v.esFactura()) {
            total(tot, "Op. gravada", soles(v.getOpGravada()), normal);
            total(tot, "IGV (18%)", soles(v.getIgv()), normal);
        }
        total(tot, "IMPORTE TOTAL", soles(v.getTotal()), totalF);
        if ("EFECTIVO".equals(v.getMetodoPago())) {
            total(tot, "Efectivo recibido", soles(v.getMontoRecibido()), normal);
            total(tot, "Vuelto", soles(v.getVuelto()), normal);
        }
        doc.add(tot);

        if (!v.esFactura()) {
            Paragraph igv = new Paragraph("Precios incluyen IGV (18%): S/ " + v.getIgv(), chico);
            igv.setSpacingBefore(6);
            doc.add(igv);
        }

        // ── Pie ──
        PdfPTable pie = new PdfPTable(1);
        pie.setWidthPercentage(100);
        pie.setSpacingBefore(24);
        PdfPCell pc = celda("Atendido por: " + v.getVendedor().getNombreCompleto()
                + "   ·   Representación impresa del comprobante electrónico   ·   ¡Gracias por su compra!", chico, Element.ALIGN_CENTER);
        pc.setBorder(Rectangle.TOP);
        pc.setBorderColor(HILO);
        pc.setBorderWidth(1.5f);
        pc.setPaddingTop(8);
        pie.addCell(pc);
        doc.add(pie);

        doc.close();
        return out.toByteArray();
    }

    /* ── Ayudantes ── */

    private static String soles(BigDecimal monto) {
        return "S/ " + String.format(java.util.Locale.US, "%,.2f", monto == null ? BigDecimal.ZERO : monto);
    }

    private static PdfPCell celda(String texto, Font f, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5);
        c.setHorizontalAlignment(alineacion);
        return c;
    }

    private static PdfPCell fondo(PdfPCell c, Color color) {
        c.setBackgroundColor(color);
        return c;
    }

    private static void fila(PdfPTable t, String etiqueta, String valor, Font fe, Font fv) {
        t.addCell(celda(etiqueta, fe, Element.ALIGN_LEFT));
        t.addCell(celda(valor == null ? "—" : valor, fv, Element.ALIGN_LEFT));
    }

    private static void total(PdfPTable t, String etiqueta, String valor, Font f) {
        t.addCell(celda("", f, Element.ALIGN_LEFT));
        t.addCell(celda(etiqueta, f, Element.ALIGN_RIGHT));
        t.addCell(celda(valor, f, Element.ALIGN_RIGHT));
    }
}
