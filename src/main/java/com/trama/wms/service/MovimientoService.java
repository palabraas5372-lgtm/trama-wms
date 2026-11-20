package com.trama.wms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.dto.MovimientoForm;
import com.trama.wms.model.DetalleMovimiento;
import com.trama.wms.model.Movimiento;
import com.trama.wms.model.Producto;
import com.trama.wms.model.Proveedor;
import com.trama.wms.model.Sucursal;
import com.trama.wms.model.Usuario;
import com.trama.wms.repository.MovimientoRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.ProveedorRepository;
import com.trama.wms.repository.SucursalRepository;
import com.trama.wms.repository.UsuarioRepository;

/**
 * Alta, edicion y baja de movimientos con su detalle de productos. Cada linea suma
 * o descuenta el stock del producto en la misma transaccion, de modo que el
 * inventario y el historial nunca se contradicen. Al editar se revierte primero el
 * efecto del registro original y al eliminar se devuelve el stock movido.
 */
@Service
public class MovimientoService {

    private static final String ENTRADA = "entrada";

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final SucursalRepository sucursalRepository;

    public MovimientoService(MovimientoRepository movimientoRepository, ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository, ProveedorRepository proveedorRepository,
            SucursalRepository sucursalRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.proveedorRepository = proveedorRepository;
        this.sucursalRepository = sucursalRepository;
    }

    @Transactional
    public void registrar(MovimientoForm form) {

        String tipo = form.getTipo() == null ? "" : form.getTipo().trim().toLowerCase(Locale.forLanguageTag("es"));
        if (!ENTRADA.equals(tipo) && !"salida".equals(tipo)) {
            throw new MovimientoInvalidoException("Indica si el movimiento es una entrada o una salida.");
        }
        boolean entrada = ENTRADA.equals(tipo);

        Map<Integer, Integer> items = itemsDelFormulario(form);
        if (items.isEmpty()) {
            throw new MovimientoInvalidoException("Agrega al menos un producto con su cantidad.");
        }

        if (form.getUsuarioId() == null) {
            throw new MovimientoInvalidoException("Selecciona el usuario responsable del movimiento.");
        }
        Usuario usuario = usuarioRepository.findById(form.getUsuarioId())
                .orElseThrow(() -> new MovimientoInvalidoException("El usuario seleccionado ya no existe."));

        // En una entrada el origen es el proveedor; en una salida, la sucursal destino.
        Proveedor proveedor = null;
        Sucursal sucursal = null;
        if (entrada) {
            if (form.getProveedorId() == null) {
                throw new MovimientoInvalidoException("Selecciona el proveedor de la entrada.");
            }
            proveedor = proveedorRepository.findById(form.getProveedorId())
                    .orElseThrow(() -> new MovimientoInvalidoException("El proveedor ya no existe."));
        } else {
            if (form.getSucursalId() == null) {
                throw new MovimientoInvalidoException("Selecciona la sucursal de destino de la salida.");
            }
            sucursal = sucursalRepository.findById(form.getSucursalId())
                    .orElseThrow(() -> new MovimientoInvalidoException("La sucursal ya no existe."));
        }

        // Se valida el stock de todo el detalle antes de tocar nada: si una linea no
        // alcanza, no se descuenta ninguna y el inventario queda intacto.
        Map<Integer, Producto> productos = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> item : items.entrySet()) {
            Producto producto = productoRepository.findById(item.getKey())
                    .orElseThrow(() -> new MovimientoInvalidoException("Uno de los productos ya no existe."));
            if (!entrada && item.getValue() > producto.getStock()) {
                throw new MovimientoInvalidoException("Stock insuficiente de " + producto.getCodigo() + " "
                        + producto.getNombre() + ": hay " + producto.getStock() + " unidades y se piden "
                        + item.getValue() + ".", true);
            }
            productos.put(item.getKey(), producto);
        }

        Movimiento movimiento;
        if (form.getIdMovimiento() != null) {
            movimiento = movimientoRepository.findById(form.getIdMovimiento())
                    .orElseThrow(() -> new MovimientoInvalidoException("El movimiento que editas ya no existe."));
            devolverStock(movimiento);
            movimiento.getDetalles().clear();
        } else {
            movimiento = new Movimiento();
            movimiento.setDetalles(new ArrayList<>());
        }

        movimiento.setTipo(tipo);
        movimiento.setFecha(form.getFecha() == null ? LocalDateTime.now() : form.getFecha());
        movimiento.setUsuario(usuario);
        movimiento.setProveedor(proveedor);
        movimiento.setSucursal(sucursal);
        movimiento.setObservacion(form.getObservacion());
        movimientoRepository.save(movimiento);

        for (Map.Entry<Integer, Integer> item : items.entrySet()) {
            Producto producto = productos.get(item.getKey());
            int cantidad = item.getValue();

            producto.setStock(Math.max(0, producto.getStock() + movimiento.efectoSobreStock(cantidad)));
            productoRepository.save(producto);

            DetalleMovimiento detalle = new DetalleMovimiento();
            detalle.setMovimiento(movimiento);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            movimiento.getDetalles().add(detalle);
        }
    }

    /** Elimina el movimiento y devuelve al inventario las unidades que habia movido. */
    @Transactional
    public void eliminar(Integer id) {
        Movimiento movimiento = movimientoRepository.findById(id).orElse(null);
        if (movimiento == null) {
            return;
        }
        devolverStock(movimiento);
        movimientoRepository.delete(movimiento);
    }

    /**
     * Invierte el efecto de un movimiento sobre el stock de sus productos. Se usa al
     * editar (para reemplazar el detalle) y al eliminar (para deshacerlo).
     */
    private void devolverStock(Movimiento movimiento) {
        if (movimiento.getDetalles() == null) {
            return;
        }
        for (DetalleMovimiento detalle : movimiento.getDetalles()) {
            Producto producto = productoRepository.findById(detalle.getProducto().getIdProducto()).orElse(null);
            if (producto == null) {
                continue;
            }
            producto.setStock(Math.max(0, producto.getStock() - movimiento.efectoSobreStock(detalle.getCantidad())));
            productoRepository.save(producto);
        }
    }

    /** Empareja las dos listas del formulario y acumula las lineas repetidas. */
    private Map<Integer, Integer> itemsDelFormulario(MovimientoForm form) {
        List<Integer> ids = form.getProductoIds();
        List<Integer> cantidades = form.getCantidades();

        Map<Integer, Integer> items = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(ids.size(), cantidades.size()); i++) {
            Integer idProducto = ids.get(i);
            Integer cantidad = cantidades.get(i);
            if (idProducto == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            items.merge(idProducto, cantidad, Integer::sum);
        }
        return items;
    }

}
