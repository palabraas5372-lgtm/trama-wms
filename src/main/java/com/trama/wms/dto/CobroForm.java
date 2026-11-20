package com.trama.wms.dto;

import java.math.BigDecimal;

/** Datos del modal "Cobrar pedido" del administrador. */
public class CobroForm {

    private String metodoPago;      // EFECTIVO, TRANSFERENCIA, YAPE_PLIN, TARJETA
    private String tipoComprobante; // BOLETA o FACTURA
    private String ruc;
    private String razonSocial;
    private BigDecimal montoRecibido;

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal montoRecibido) { this.montoRecibido = montoRecibido; }
}
