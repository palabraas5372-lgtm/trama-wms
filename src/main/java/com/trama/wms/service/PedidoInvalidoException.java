package com.trama.wms.service;

/** Error de validacion al crear o cambiar un pedido (sin stock, cantidad invalida, etc.). */
public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }

}
