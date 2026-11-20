package com.trama.wms.dto;

import java.math.BigDecimal;

/** Una fila del reporte de ventas: un mes de un año. */
public record ReporteMes(int anio, int mes, String nombreMes, int ventas, int unidades,
        BigDecimal subtotal, BigDecimal descuentos, BigDecimal total) {
}
