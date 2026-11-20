package com.trama.wms.dto;


public record MovimientoLinea(String fecha, String tipo, String codigo, String producto, String variante,
        String cantidad, String variacion, String stock, String origen, String usuario) {

    public boolean esEntrada() {
        return "entrada".equalsIgnoreCase(tipo);
    }

}
