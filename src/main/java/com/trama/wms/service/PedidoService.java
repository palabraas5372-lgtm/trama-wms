package com.trama.wms.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.model.Descuento;
import com.trama.wms.model.Pedido;
import com.trama.wms.model.Producto;
import com.trama.wms.model.Usuario;
import com.trama.wms.repository.PedidoRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.UsuarioRepository;

@Service
public class PedidoService {


    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioService calendario;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, CalendarioService calendario) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
    }

    /** El cliente arma su pedido con una o varias lineas (producto + cantidad). */
    @Transactional
    public Pedido crear(Integer idUsuario, List<Integer> productos, List<Integer> cantidades,
            String telefono, String observacion) {

        if (productos == null || productos.isEmpty() || cantidades == null
                || productos.size() != cantidades.size()) {
            throw new PedidoInvalidoException("Agrega al menos un producto al pedido.");
        }
        String tel = telefono == null ? "" : telefono.trim();
        if (!tel.matches("[0-9 +()-]{6,20}")) {
            throw new PedidoInvalidoException("Ingresa un teléfono de contacto válido.");
        }

        // Si el mismo producto viene dos veces, se suman las cantidades
        Map<Integer, Integer> lineas = new LinkedHashMap<>();
        for (int i = 0; i < productos.size(); i++) {
            Integer cant = cantidades.get(i);
            if (productos.get(i) == null || cant == null || cant <= 0) {
                throw new PedidoInvalidoException("Todas las cantidades deben ser mayores que cero.");
            }
            lineas.merge(productos.get(i), cant, Integer::sum);
        }
        // Al por mayor: mínimo una decena de cada producto
        for (Integer cantidad : lineas.values()) {
            if (cantidad < Pedido.MINIMO_POR_PRODUCTO) {
                throw new PedidoInvalidoException("Cada producto se pide como mínimo por "
                        + Pedido.MINIMO_POR_PRODUCTO + " unidades (venta al por mayor).");
            }
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new PedidoInvalidoException("Tu sesión ya no es válida."));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setSucursal(usuario.getSucursal());   // almacén elegido en el registro
        pedido.setTelefono(tel);
        pedido.setObservacion(observacion == null || observacion.isBlank() ? null : observacion.trim());
        // Descuento adicional si el pedido se hace durante una fecha especial
        Descuento.Festivo festivo = calendario.festivoVigente();
        if (festivo != null) {
            pedido.setFestivoNombre(festivo.nombre());
            pedido.setFestivoPct(festivo.porcentaje());
        }

        for (Map.Entry<Integer, Integer> linea : lineas.entrySet()) {
            Producto p = productoRepository.findById(linea.getKey())
                    .orElseThrow(() -> new PedidoInvalidoException("Uno de los productos ya no existe."));
            if (linea.getValue() > p.getStock()) {
                throw new PedidoInvalidoException("Solo hay " + p.getStock() + " unidades de "
                        + p.getNombre() + " (" + p.getTalla() + ", " + p.getColor() + ").");
            }
            pedido.agregar(p, linea.getValue());
        }

        return pedidoRepository.save(pedido);
    }

    /** El cliente solo puede cancelar SUS pedidos y solo mientras esten pendientes. */
    @Transactional
    public Pedido cancelarPorCliente(Integer idPedido, Integer idUsuario) {
        Pedido p = pedidoRepository.findById(idPedido)
                .filter(x -> x.getUsuario().getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new PedidoInvalidoException("Pedido no encontrado."));
        if (!Pedido.PENDIENTE.equals(p.getEstado())) {
            throw new PedidoInvalidoException("Solo se pueden cancelar pedidos pendientes.");
        }
        p.setEstado(Pedido.CANCELADO);
        return pedidoRepository.save(p);
    }

    /**
     * El administrador cancela un pedido pendiente. (Cobrar y entregar los hace
     * VentaService, porque generan la venta y descuentan el stock.)
     */
    @Transactional
    public Pedido cancelarPorAdministrador(Integer idPedido) {
        Pedido p = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoInvalidoException("Pedido no encontrado."));
        if (!Pedido.PENDIENTE.equals(p.getEstado())) {
            throw new PedidoInvalidoException("Solo se pueden cancelar pedidos pendientes; un pedido cobrado ya es una venta.");
        }
        p.setEstado(Pedido.CANCELADO);
        return pedidoRepository.save(p);
    }

}
