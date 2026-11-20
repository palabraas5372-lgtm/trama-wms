package com.trama.wms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.dto.MovimientoFila;
import com.trama.wms.dto.MovimientoLinea;
import com.trama.wms.dto.PanelOperacion;
import com.trama.wms.model.DetalleMovimiento;
import com.trama.wms.model.Movimiento;
import com.trama.wms.model.Producto;
import com.trama.wms.repository.MovimientoRepository;
import com.trama.wms.repository.ProductoRepository;

/**
 * Lecturas del modulo: historial de movimientos con su detalle y las cifras del panel
 * de control. Todo se arma en Java a partir de las tablas, para que el dashboard
 * muestre informacion real del movimiento y no datos de ejemplo.
 */
@Service
public class ReporteService {

    private static final DateTimeFormatter FORMATO_MOVIMIENTO = DateTimeFormatter
            .ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es"));
    private static final DateTimeFormatter FORMATO_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("EEE dd", Locale.forLanguageTag("es"));

    private final MovimientoRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    public ReporteService(MovimientoRepository movimientoRepository, ProductoRepository productoRepository) {
        this.movimientoRepository = movimientoRepository;
        this.productoRepository = productoRepository;
    }

    /** Historial completo con el detalle de cada movimiento y el stock que dejo cada linea. */
    @Transactional(readOnly = true)
    public List<MovimientoFila> listarMovimientos() {
        return aFilas(movimientoRepository.findAllByOrderByFechaDesc());
    }

    /** Una linea por producto movido en los ultimos dias, para el bloque del dashboard. */
    @Transactional(readOnly = true)
    public List<MovimientoLinea> lineasRecientes(int dias, int limite) {
        List<Movimiento> movimientos = movimientoRepository.findDesde(inicioDeSemana(dias));
        Map<String, Integer> stockTras = stockResultante(movimientos);

        List<MovimientoLinea> lineas = new ArrayList<>();
        for (Movimiento movimiento : movimientos) {
            for (DetalleMovimiento detalle : detalles(movimiento)) {
                Producto producto = detalle.getProducto();
                String clave = clave(detalle);
                lineas.add(new MovimientoLinea(
                        formatear(movimiento.getFecha()),
                        movimiento.getTipo(),
                        producto.getCodigo(),
                        producto.getNombre(),
                        variante(producto),
                        String.valueOf(detalle.getCantidad()),
                        variacion(movimiento, detalle.getCantidad()),
                        unidades(stockTras.getOrDefault(clave, producto.getStock())),
                        origen(movimiento),
                        movimiento.getUsuario() == null ? "—" : movimiento.getUsuario().getNombreCompleto()));
            }
        }
        return lineas.stream().limit(limite).collect(Collectors.toList());
    }

    /** Cifras y series del panel: unidades en stock, flujo de la semana y top de rotacion. */
    @Transactional(readOnly = true)
    public PanelOperacion panel(int dias) {

        List<Producto> productos = productoRepository.findAll();
        List<Movimiento> semana = movimientoRepository.findDesde(inicioDeSemana(dias));
        Map<String, Integer> stockTras = stockResultante(semana);

        // Flujo por dia: los dias sin movimiento se rellenan con cero para que la
        // serie del grafico no tenga huecos.
        LocalDate hoy = LocalDate.now();
        Map<LocalDate, Integer> entradasDia = new LinkedHashMap<>();
        Map<LocalDate, Integer> salidasDia = new LinkedHashMap<>();
        List<LocalDate> ventana = new ArrayList<>();
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            ventana.add(dia);
            entradasDia.put(dia, 0);
            salidasDia.put(dia, 0);
        }

        int entradas = 0;
        int salidas = 0;
        int unidadesEntradas = 0;
        int unidadesSalidas = 0;

        for (Movimiento movimiento : semana) {
            LocalDate dia = movimiento.getFecha().toLocalDate();
            boolean entrada = movimiento.esEntrada();
            if (entrada) {
                entradas++;
            } else {
                salidas++;
            }
            for (DetalleMovimiento detalle : detalles(movimiento)) {
                int unidades = detalle.getCantidad();
                if (entrada) {
                    unidadesEntradas += unidades;
                    entradasDia.merge(dia, unidades, Integer::sum);
                } else {
                    unidadesSalidas += unidades;
                    salidasDia.merge(dia, unidades, Integer::sum);
                }
            }
        }

        // Productos que mas rotaron en la ventana, sumando entradas y salidas.
        Map<String, int[]> rotacion = new LinkedHashMap<>();
        Map<String, String> rotacionEtiqueta = new LinkedHashMap<>();
        for (Movimiento movimiento : semana) {
            for (DetalleMovimiento detalle : detalles(movimiento)) {
                Producto producto = detalle.getProducto();
                rotacionEtiqueta.put(producto.getCodigo(), producto.getNombre());
                rotacion.computeIfAbsent(producto.getCodigo(), k -> new int[1])[0] += detalle.getCantidad();
            }
        }
        List<String> topRotacion = rotacion.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, int[]> e) -> -e.getValue()[0]))
                .limit(6)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new PanelOperacion(
                productos.stream().mapToInt(p -> p.getStock() == null ? 0 : p.getStock()).sum(),
                (int) productos.stream().filter(p -> p.getStock() != null && p.getStock() <= p.getStockMinimo()).count(),
                semana.size(),
                entradas,
                salidas,
                unidadesEntradas,
                unidadesSalidas,
                ventana.stream().map(d -> d.format(FORMATO_DIA)).collect(Collectors.toList()),
                ventana.stream().map(entradasDia::get).collect(Collectors.toList()),
                ventana.stream().map(salidasDia::get).collect(Collectors.toList()),
                topRotacion.stream().map(codigo -> rotacionEtiqueta.getOrDefault(codigo, codigo)).collect(Collectors.toList()),
                topRotacion.stream().map(codigo -> rotacion.get(codigo)[0]).collect(Collectors.toList()));
    }

    private List<MovimientoFila> aFilas(List<Movimiento> movimientos) {
        Map<String, Integer> stockTras = stockResultante(movimientos);
        return movimientos.stream().map(movimiento -> aFila(movimiento, stockTras)).collect(Collectors.toList());
    }

    private MovimientoFila aFila(Movimiento movimiento, Map<String, Integer> stockTras) {

        List<MovimientoFila.Linea> lineas = new ArrayList<>();
        for (DetalleMovimiento detalle : detalles(movimiento)) {
            Producto producto = detalle.getProducto();
            lineas.add(new MovimientoFila.Linea(
                    producto.getIdProducto(),
                    producto.getCodigo(),
                    producto.getNombre(),
                    variante(producto),
                    detalle.getCantidad(),
                    variacion(movimiento, detalle.getCantidad()),
                    unidades(stockTras.getOrDefault(clave(detalle), producto.getStock()))));
        }

        return new MovimientoFila(
                movimiento.getIdMovimiento(),
                formatear(movimiento.getFecha()),
                movimiento.getFecha() == null ? "" : movimiento.getFecha().format(FORMATO_ISO),
                movimiento.getTipo(),
                origen(movimiento),
                movimiento.getUsuario() == null ? "—" : movimiento.getUsuario().getNombreCompleto(),
                movimiento.getUsuario() == null ? null : movimiento.getUsuario().getIdUsuario(),
                movimiento.getSucursal() == null ? null : movimiento.getSucursal().getIdSucursal(),
                movimiento.getProveedor() == null ? null : movimiento.getProveedor().getIdProveedor(),
                movimiento.getObservacion(),
                lineas.stream().mapToInt(MovimientoFila.Linea::cantidad).sum(),
                lineas.size(),
                detalleJson(lineas),
                lineas);
    }

    /** Movimientos grabados sin detalle (formularios antiguos) se leen como lista vacia. */
    private List<DetalleMovimiento> detalles(Movimiento movimiento) {
        return movimiento.getDetalles() == null ? List.of() : movimiento.getDetalles();
    }

    /**
     * Stock de cada producto justo despues de cada linea de detalle. Se parte del
     * stock actual y se retrocede en el tiempo, de la linea mas reciente a la mas
     * antigua, restando el efecto del movimiento en cada paso.
     */
    private Map<String, Integer> stockResultante(List<Movimiento> movimientos) {

        Map<Integer, Integer> stockActual = productoRepository.findAll().stream()
                .collect(Collectors.toMap(Producto::getIdProducto, p -> p.getStock() == null ? 0 : p.getStock()));

        Map<Integer, List<Paso>> porProducto = new HashMap<>();
        for (Movimiento movimiento : movimientos) {
            for (DetalleMovimiento detalle : detalles(movimiento)) {
                porProducto.computeIfAbsent(detalle.getProducto().getIdProducto(), k -> new ArrayList<>())
                        .add(new Paso(movimiento, detalle));
            }
        }

        Map<String, Integer> resultado = new HashMap<>();
        porProducto.forEach((idProducto, pasos) -> {
            pasos.sort(Comparator.comparing((Paso p) -> p.movimiento().getFecha())
                    .thenComparing(p -> p.movimiento().getIdMovimiento())
                    .reversed());
            int stock = stockActual.getOrDefault(idProducto, 0);
            for (Paso paso : pasos) {
                resultado.put(clave(paso.detalle()), Math.max(0, stock));
                stock -= paso.movimiento().efectoSobreStock(paso.detalle().getCantidad());
            }
        });
        return resultado;
    }

    private LocalDateTime inicioDeSemana(int dias) {
        return LocalDateTime.now().minusDays(dias - 1L).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private String origen(Movimiento movimiento) {
        boolean entrada = movimiento.esEntrada();
        if (entrada && movimiento.getProveedor() != null) {
            return movimiento.getProveedor().getNombre();
        }
        if (!entrada && movimiento.getSucursal() != null) {
            return movimiento.getSucursal().getNombre();
        }
        return "Almacén central";
    }

    private String variante(Producto producto) {
        String color = producto.getColor() == null ? "" : producto.getColor();
        String talla = producto.getTalla() == null ? "" : producto.getTalla();
        String variante = (color.isEmpty() ? "" : color) + (talla.isEmpty() ? "" : " / " + talla);
        return variante.isEmpty() ? "—" : variante;
    }

    private String variacion(Movimiento movimiento, int cantidad) {
        return (movimiento.esEntrada() ? "+" : "−") + cantidad;
    }

    private String unidades(int cantidad) {
        return cantidad + " uds.";
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "—" : fecha.format(FORMATO_MOVIMIENTO);
    }

    private String clave(DetalleMovimiento detalle) {
        return detalle.getMovimiento().getIdMovimiento() + ":" + detalle.getIdDetalle();
    }

    /** El detalle serializado para que el modal de edicion lo recupere del boton. */
    private String detalleJson(List<MovimientoFila.Linea> lineas) {
        return lineas.stream()
                .map(l -> "[" + l.productoId() + "," + l.cantidad() + "]")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private record Paso(Movimiento movimiento, DetalleMovimiento detalle) {
    }

}
