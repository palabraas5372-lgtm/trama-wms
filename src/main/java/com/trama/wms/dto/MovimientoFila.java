package com.trama.wms.dto;

import java.util.List;
import java.util.stream.Collectors;


public record MovimientoFila(Integer id, String fecha, String fechaIso, String tipo, String origen, String usuario,
        Integer usuarioId, Integer sucursalId, Integer proveedorId, String observacion, int unidades, int items,
        String detalleJson, List<Linea> detalles) {

   
    public record Linea(Integer productoId, String codigo, String nombre, String variante, int cantidad,
            String variacion, String stock) {
    }

    public boolean esEntrada() {
        return "entrada".equalsIgnoreCase(tipo);
    }

    public String etiquetaTipo() {
        return esEntrada() ? "Entrada" : "Salida";
    }

    public String resumenDetalle() {
        return detalles.stream()
                .map(l -> l.codigo() + " ×" + l.cantidad())
                .collect(Collectors.joining(", "));
    }

}
