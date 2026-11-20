package com.trama.wms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Venta generada al COBRAR un pedido de cliente. Guarda el comprobante (boleta o
 * factura), el método de pago, el descuento aplicado y el precio de cada producto
 * en el momento de la venta.
 */
@Entity
@Table(name = "ventas")
public class Venta {

    public static final String BOLETA = "BOLETA";
    public static final String FACTURA = "FACTURA";
    public static final BigDecimal IGV = new BigDecimal("0.18");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Usuario cliente;

    /** Administrador que cobró. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "tipo_comprobante", nullable = false, length = 10)
    private String tipoComprobante = BOLETA;

    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 120)
    private String razonSocial;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago;

    @Column(name = "monto_recibido", precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_pct", nullable = false)
    private Integer descuentoPct = 0;

    @Column(name = "festivo_nombre", length = 40)
    private String festivoNombre;

    @Column(name = "festivo_pct")
    private Integer festivoPct;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    /** Número de comprobante: B001-00000001 / F001-00000001. */
    public String getComprobante() {
        return serie + "-" + String.format("%08d", numero == null ? 0 : numero);
    }

    public boolean esFactura() {
        return FACTURA.equals(tipoComprobante);
    }

    /** Total sin IGV (los precios ya incluyen IGV). */
    public BigDecimal getOpGravada() {
        return total.divide(BigDecimal.ONE.add(IGV), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getIgv() {
        return total.subtract(getOpGravada());
    }

    public BigDecimal getVuelto() {
        return montoRecibido == null ? BigDecimal.ZERO : montoRecibido.subtract(total).max(BigDecimal.ZERO);
    }

    public int getTotalUnidades() {
        return detalles.stream().mapToInt(DetalleVenta::getCantidad).sum();
    }

    public void agregar(Producto producto, int cantidad) {
        DetalleVenta d = new DetalleVenta();
        d.setVenta(this);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(producto.getPrecio());
        detalles.add(d);
        subtotal = subtotal.add(d.getImporte());
    }

    /** Aplica el descuento por volumen + el de la fecha especial del pedido y calcula el total. */
    public void aplicarDescuento(String festivo, int pctFestivo) {
        festivoNombre = pctFestivo > 0 ? festivo : null;
        festivoPct = pctFestivo;
        descuentoPct = Descuento.porcentaje(getTotalUnidades()) + pctFestivo;
        descuento = Descuento.monto(subtotal, descuentoPct);
        total = subtotal.subtract(descuento);
    }

    public int getVolumenPct() {
        return descuentoPct - getFestivoPctSeguro();
    }

    public int getFestivoPctSeguro() {
        return festivoPct == null ? 0 : festivoPct;
    }

    public BigDecimal getDescuentoVolumen() {
        return Descuento.monto(subtotal, getVolumenPct());
    }

    public BigDecimal getDescuentoFestivo() {
        return descuento.subtract(getDescuentoVolumen());
    }

    public Integer getIdVenta() { return idVenta; }
    public void setIdVenta(Integer idVenta) { this.idVenta = idVenta; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }

    public Sucursal getSucursal() { return sucursal; }
    public void setSucursal(Sucursal sucursal) { this.sucursal = sucursal; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal montoRecibido) { this.montoRecibido = montoRecibido; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Integer getDescuentoPct() { return descuentoPct; }
    public void setDescuentoPct(Integer descuentoPct) { this.descuentoPct = descuentoPct; }

    public String getFestivoNombre() { return festivoNombre; }
    public void setFestivoNombre(String festivoNombre) { this.festivoNombre = festivoNombre; }

    public Integer getFestivoPct() { return festivoPct; }
    public void setFestivoPct(Integer festivoPct) { this.festivoPct = festivoPct; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}
