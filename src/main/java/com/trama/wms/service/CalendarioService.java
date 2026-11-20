package com.trama.wms.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.trama.wms.model.Descuento;

/**
 * Fecha "de hoy" para los descuentos por fechas especiales. Para la exposición se
 * puede simular un día en application.properties, por ejemplo:
 *   trama.descuentos.fecha-simulada=2026-12-20   (campaña de Navidad)
 * Si se deja vacío se usa la fecha real.
 */
@Service
public class CalendarioService {

    @Value("${trama.descuentos.fecha-simulada:}")
    private String fechaSimulada;

    public LocalDate hoy() {
        if (fechaSimulada != null && !fechaSimulada.isBlank()) {
            try {
                return LocalDate.parse(fechaSimulada.trim());
            } catch (RuntimeException e) {
                // formato inválido: se usa la fecha real
            }
        }
        return LocalDate.now();
    }

    public boolean esSimulada() {
        return fechaSimulada != null && !fechaSimulada.isBlank();
    }

    public Descuento.Festivo festivoVigente() {
        return Descuento.festivoVigente(hoy());
    }

    public Descuento.Festivo proximoFestivo() {
        return Descuento.proximoFestivo(hoy());
    }
}
