package com.trama.wms.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.trama.wms.model.Venta;
import com.trama.wms.service.ComprobantePdfService;

/** Comprobantes en PDF para el administrador (el cliente usa /cliente/comprobante/{id}). */
@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final ComprobantePdfService pdfService;

    public VentaController(ComprobantePdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Integer id) {
        return respuestaPdf(pdfService.buscar(id), pdfService.generar(id));
    }

    /** Respuesta HTTP con el PDF para verlo en el navegador o descargarlo. */
    static ResponseEntity<byte[]> respuestaPdf(Venta v, byte[] pdf) {
        String nombre = (v.esFactura() ? "Factura_" : "Boleta_") + v.getComprobante() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(pdf);
    }
}
