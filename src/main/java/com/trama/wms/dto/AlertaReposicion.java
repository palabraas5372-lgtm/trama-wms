package com.trama.wms.dto;


public record AlertaReposicion(String codigo, String nombre, String talla, String color, Integer stock,
        Integer stockMinimo, boolean agotado) {
}
