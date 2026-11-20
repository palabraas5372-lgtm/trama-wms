package com.trama.wms.service;

/**
 * Regla de negocio del modulo de entradas y salidas que impide registrar el
 * movimiento (stock insuficiente, producto inexistente, detalle vacio...).
 * El controller la traduce a un aviso en pantalla y no deja el stock a medias.
 */
public class MovimientoInvalidoException extends RuntimeException {

    private final boolean stockInsuficiente;

    public MovimientoInvalidoException(String mensaje) {
        this(mensaje, false);
    }

    public MovimientoInvalidoException(String mensaje, boolean stockInsuficiente) {
        super(mensaje);
        this.stockInsuficiente = stockInsuficiente;
    }

    public boolean esStockInsuficiente() {
        return stockInsuficiente;
    }

}
