package com.trama.wms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Pedido que un CLIENTE hace desde su portal. Flujo:
 * pendiente → pagado (el administrador cobra y se genera la venta con su comprobante)
 * → entregado. Un pedido pendiente también puede cancelarse.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    public static final String PENDIENTE = "pendiente";
    public static final String PAGADO = "pagado";
    public static final String ENTREGADO = "entregado";
    /** Estado de versiones anteriores (antes de existir el cobro). */
    public static final String ATENDIDO = "atendido";
    public static final String CANCELADO = "cancelado";

    /** Venta al por mayor: cada producto del pedido se pide como mínimo por decena. */
    public static final int MINIMO_POR_PRODUCTO = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Almacén (sucursal) que atiende el pedido: el que el cliente eligió al registrarse. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = PENDIENTE;

    /** Campaña por fecha especial vigente al crear el pedido (Navidad, Fiestas Patrias...). */
    @Column(name = "festivo_nombre", length = 40)
    private String festivoNombre;

    @Column(name = "festivo_pct")
    private Integer festivoPct;

    @Column(name = "entregado_en")
    private LocalDateTime entregadoEn;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "observacion", length = 200)
    private String observacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    /** Codigo visible: PED-0001, PED-0002... (se calcula a partir del id). */
    public String getCodigo() {
        return idPedido == null ? "PED-NUEVO" : String.format("PED-%04d", idPedido);
    }

    public int getTotalUnidades() {
        return detalles.stream().mapToInt(DetallePedido::getCantidad).sum();
    }

    /* ── Montos estimados con el precio actual (el monto final lo fija la venta) ── */

    public BigDecimal getSubtotalEstimado() {
        return detalles.stream()
                .map(d -> d.getProducto().getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Descuento total = volumen + fecha especial. */
    public int getDescuentoPct() {
        return getDescuentoVolumenPct() + getDescuentoFestivoPct();
    }

    public int getDescuentoVolumenPct() {
        return Descuento.porcentaje(getTotalUnidades());
    }

    public int getDescuentoFestivoPct() {
        return festivoPct == null ? 0 : festivoPct;
    }

    public BigDecimal getDescuentoEstimado() {
        return Descuento.monto(getSubtotalEstimado(), getDescuentoPct());
    }

    public BigDecimal getTotalEstimado() {
        return getSubtotalEstimado().subtract(getDescuentoEstimado());
    }

    public void agregar(Producto producto, int cantidad) {
        DetallePedido d = new DetallePedido();
        d.setPedido(this);
        d.setProducto(producto);
        d.setCantidad(cantidad);
        detalles.add(d);
    }

    public Integer getIdPedido() { return idPedido; }
    public void setIdPedido(Integer idPedido) { this.idPedido = idPedido; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Sucursal getSucursal() { return sucursal; }
    public void setSucursal(Sucursal sucursal) { this.sucursal = sucursal; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getFestivoNombre() { return festivoNombre; }
    public void setFestivoNombre(String festivoNombre) { this.festivoNombre = festivoNombre; }

    public Integer getFestivoPct() { return festivoPct; }
    public void setFestivoPct(Integer festivoPct) { this.festivoPct = festivoPct; }

    public LocalDateTime getEntregadoEn() { return entregadoEn; }
    public void setEntregadoEn(LocalDateTime entregadoEn) { this.entregadoEn = entregadoEn; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}
