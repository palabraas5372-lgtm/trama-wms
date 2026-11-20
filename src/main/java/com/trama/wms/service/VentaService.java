package com.trama.wms.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.dto.CobroForm;
import com.trama.wms.model.DetallePedido;
import com.trama.wms.model.Pedido;
import com.trama.wms.model.Producto;
import com.trama.wms.model.Usuario;
import com.trama.wms.model.Venta;
import com.trama.wms.repository.PedidoRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.UsuarioRepository;
import com.trama.wms.repository.VentaRepository;

/**
 * Cierre del flujo de venta:
 *   1. COBRAR: valida el pago y el stock, descuenta el inventario, registra la venta
 *      con su comprobante (boleta o factura) y deja el pedido "pagado".
 *   2. ENTREGAR: cambia el pedido pagado a "entregado".
 * Cada paso se ejecuta en una transacción: si algo falla, no se guarda nada.
 */
@Service
public class VentaService {

    public static final Map<String, String> METODOS_PAGO = Map.of(
            "EFECTIVO", "Efectivo",
            "TRANSFERENCIA", "Transferencia bancaria",
            "YAPE_PLIN", "Yape / Plin",
            "TARJETA", "Tarjeta de crédito o débito");

    private static final Set<String> TIPOS = Set.of(Venta.BOLETA, Venta.FACTURA);

    private final VentaRepository ventaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public VentaService(VentaRepository ventaRepository, PedidoRepository pedidoRepository,
            ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.ventaRepository = ventaRepository;
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Venta cobrar(Integer idPedido, Integer idAdministrador, CobroForm form) {

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoInvalidoException("Pedido no encontrado."));
        if (!Pedido.PENDIENTE.equals(pedido.getEstado())) {
            throw new PedidoInvalidoException("Solo se pueden cobrar pedidos pendientes.");
        }
        if (ventaRepository.findByPedido_IdPedido(idPedido).isPresent()) {
            throw new PedidoInvalidoException("Este pedido ya fue cobrado.");
        }

        // ── Datos del pago y del comprobante ──
        String metodo = form.getMetodoPago() == null ? "" : form.getMetodoPago().trim().toUpperCase();
        if (!METODOS_PAGO.containsKey(metodo)) {
            throw new PedidoInvalidoException("Selecciona el método de pago.");
        }
        String tipo = form.getTipoComprobante() == null ? Venta.BOLETA : form.getTipoComprobante().trim().toUpperCase();
        if (!TIPOS.contains(tipo)) {
            throw new PedidoInvalidoException("Tipo de comprobante no válido.");
        }
        String ruc = null, razon = null;
        if (Venta.FACTURA.equals(tipo)) {
            ruc = form.getRuc() == null ? "" : form.getRuc().trim();
            razon = form.getRazonSocial() == null ? "" : form.getRazonSocial().trim();
            if (!ruc.matches("(10|20)\\d{9}")) {
                throw new PedidoInvalidoException("Para la factura ingresa un RUC válido de 11 dígitos (empieza con 10 o 20).");
            }
            if (razon.isEmpty()) {
                throw new PedidoInvalidoException("Para la factura ingresa la razón social.");
            }
        }

        // ── Stock de TODOS los productos antes de tocar nada ──
        List<String> faltantes = new ArrayList<>();
        for (DetallePedido d : pedido.getDetalles()) {
            Producto p = d.getProducto();
            if (p.getStock() < d.getCantidad()) {
                faltantes.add(p.getCodigo() + " (pide " + d.getCantidad() + ", hay " + p.getStock() + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new PedidoInvalidoException("Stock insuficiente: " + String.join(", ", faltantes)
                    + ". Registra una entrada o cancela el pedido.");
        }

        Usuario vendedor = usuarioRepository.findById(idAdministrador)
                .orElseThrow(() -> new PedidoInvalidoException("Tu sesión ya no es válida."));

        // ── Armar la venta y descontar el stock ──
        Venta venta = new Venta();
        venta.setPedido(pedido);
        venta.setCliente(pedido.getUsuario());
        venta.setVendedor(vendedor);
        venta.setSucursal(pedido.getSucursal());
        venta.setMetodoPago(metodo);
        venta.setTipoComprobante(tipo);
        venta.setRuc(ruc);
        venta.setRazonSocial(razon);
        for (DetallePedido d : pedido.getDetalles()) {
            Producto p = d.getProducto();
            p.setStock(p.getStock() - d.getCantidad());
            productoRepository.save(p);
            venta.agregar(p, d.getCantidad());
        }
        venta.aplicarDescuento(pedido.getFestivoNombre(), pedido.getDescuentoFestivoPct());

        if ("EFECTIVO".equals(metodo)) {
            BigDecimal recibido = form.getMontoRecibido();
            if (recibido == null || recibido.compareTo(venta.getTotal()) < 0) {
                throw new PedidoInvalidoException("El monto recibido en efectivo no cubre el total (S/ " + venta.getTotal() + ").");
            }
            venta.setMontoRecibido(recibido);
        } else {
            venta.setMontoRecibido(venta.getTotal());
        }

        // Numeración correlativa por serie: B001 para boletas, F001 para facturas
        String serie = Venta.FACTURA.equals(tipo) ? "F001" : "B001";
        venta.setSerie(serie);
        venta.setNumero(ventaRepository.ultimoNumero(serie) + 1);

        pedido.setEstado(Pedido.PAGADO);
        pedidoRepository.save(pedido);

        return ventaRepository.save(venta);
    }

    @Transactional
    public Pedido entregar(Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoInvalidoException("Pedido no encontrado."));
        if (!Pedido.PAGADO.equals(pedido.getEstado())) {
            throw new PedidoInvalidoException("Solo se pueden entregar pedidos ya cobrados.");
        }
        pedido.setEstado(Pedido.ENTREGADO);
        pedido.setEntregadoEn(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

}
