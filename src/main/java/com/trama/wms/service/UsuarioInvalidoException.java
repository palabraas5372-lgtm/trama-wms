package com.trama.wms.service;

/** Error de validacion al registrar o editar un usuario (DNI o usuario repetido, etc.). */
public class UsuarioInvalidoException extends RuntimeException {

    public UsuarioInvalidoException(String mensaje) {
        super(mensaje);
    }

}
