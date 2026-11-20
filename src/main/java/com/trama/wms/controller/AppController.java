package com.trama.wms.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.trama.wms.dto.AlertaReposicion;
import com.trama.wms.dto.MovimientoFila;
import com.trama.wms.dto.PanelOperacion;
import com.trama.wms.dto.ReporteMes;
import com.trama.wms.model.Producto;
import com.trama.wms.repository.CategoriaRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.PedidoRepository;
import com.trama.wms.repository.VentaRepository;
import com.trama.wms.repository.PerfilRepository;
import com.trama.wms.repository.ProveedorRepository;
import com.trama.wms.repository.RolRepository;
import com.trama.wms.repository.SolicitudRepository;
import com.trama.wms.repository.SucursalRepository;
import com.trama.wms.repository.UsuarioRepository;
import com.trama.wms.service.ProductoService;
import com.trama.wms.service.ReporteVentasService;
import com.trama.wms.service.VentaService;
import com.trama.wms.service.ReporteService;
import com.trama.wms.service.UsuarioService;

/**
 * Renderiza el index.html, que es el shell de la aplicacion con todos los modulos
 * embebidos. El enrutado entre modulos lo hace trama-app.js en el cliente, por lo
 * que aqui se carga de una sola vez la informacion que el index necesita, con las
 * cifras del panel calculadas sobre las tablas por ReporteService.
 */
@Controller
public class AppController {

    private static final int DIAS_PANEL = 7;

    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final SolicitudRepository solicitudRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilRepository perfilRepository;
    private final PedidoRepository pedidoRepository;
    private final ReporteService reporteService;
    private final ProductoService productoService;
    private final VentaRepository ventaRepository;
    private final ReporteVentasService reporteVentasService;

    public AppController(ProductoRepository productoRepository, SucursalRepository sucursalRepository,
            SolicitudRepository solicitudRepository, CategoriaRepository categoriaRepository,
            ProveedorRepository proveedorRepository, UsuarioRepository usuarioRepository,
            RolRepository rolRepository, PerfilRepository perfilRepository, PedidoRepository pedidoRepository,
            ReporteService reporteService, ProductoService productoService, VentaRepository ventaRepository,
            ReporteVentasService reporteVentasService) {
        this.productoRepository = productoRepository;
        this.sucursalRepository = sucursalRepository;
        this.solicitudRepository = solicitudRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.perfilRepository = perfilRepository;
        this.pedidoRepository = pedidoRepository;
        this.reporteService = reporteService;
        this.productoService = productoService;
        this.ventaRepository = ventaRepository;
        this.reporteVentasService = reporteVentasService;
    }

    @GetMapping("/index")
    public String index(@RequestParam(name = "anio", required = false) Integer anio, Model model) {

        List<Producto> productos = productoRepository.findAll();
        List<SolicitudResumen> solicitudes = solicitudRepository.findAllByOrderByFechaDesc().stream()
                .map(this::toSolicitudResumen)
                .collect(Collectors.toList());

        PanelOperacion panel = reporteService.panel(DIAS_PANEL);

        // ── Dashboard ───────────────────────────────────────────────
        model.addAttribute("kpiProductos", productos.size());
        model.addAttribute("kpiUnidades", panel.unidadesEnStock());
        model.addAttribute("kpiBajo", panel.productosPorReponer());
        model.addAttribute("kpiSucursalesActivas", sucursalRepository.countByEstado("activa"));
        model.addAttribute("kpiSucursalesTotal", sucursalRepository.count());
        model.addAttribute("kpiMovimientos", panel.movimientosSemana());
        model.addAttribute("kpiEntradas", panel.entradasSemana());
        model.addAttribute("kpiSalidas", panel.salidasSemana());
        model.addAttribute("kpiUnidadesEntradas", panel.unidadesEntradasSemana());
        model.addAttribute("kpiUnidadesSalidas", panel.unidadesSalidasSemana());
        model.addAttribute("kpiSolicitudes", solicitudRepository.countByEstado("pendiente"));

        List<AlertaReposicion> alertas = productoRepository.findRequierenReposicion().stream()
                .map(this::toAlerta)
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("alertas", alertas);

        // Movimientos reales del panel: una fila por producto movido en la semana
        model.addAttribute("recientes", reporteService.lineasRecientes(DIAS_PANEL, 8));
        model.addAttribute("diasLabels", panel.diasLabels());
        model.addAttribute("entradaPorDia", panel.unidadesEntradaDia());
        model.addAttribute("salidaPorDia", panel.unidadesSalidaDia());
        model.addAttribute("rotacionLabels", panel.rotacionLabels());
        model.addAttribute("rotacionValores", panel.rotacionValores());

        // Unidades en stock agrupadas por categoría, tal como devuelve el repositorio
        List<Object[]> porCategoria = productoRepository.unidadesPorCategoria();
        model.addAttribute("categoriasLabels", porCategoria.stream()
                .map(fila -> (String) fila[0])
                .collect(Collectors.toList()));
        model.addAttribute("categoriasValores", porCategoria.stream()
                .map(fila -> ((Number) fila[1]).intValue())
                .collect(Collectors.toList()));

        int agotados = (int) productos.stream().filter(p -> p.getStock() == 0).count();
        int bajos = (int) productos.stream()
                .filter(p -> p.getStock() > 0 && p.getStock() <= p.getStockMinimo()).count();
        model.addAttribute("inventarioOk", productos.size() - agotados - bajos);
        model.addAttribute("inventarioBajo", bajos);
        model.addAttribute("inventarioAgotado", agotados);

        // ── Listas de los modulos ───────────────────────────────────
        model.addAttribute("productosL", productos);
        model.addAttribute("siguienteCodigo", productoService.siguienteCodigo());
        model.addAttribute("categoriasL", categoriaRepository.findAll());
        model.addAttribute("proveedoresL", proveedorRepository.findAll());
        model.addAttribute("sucursalesL", sucursalRepository.findAll());
        model.addAttribute("usuariosL", usuarioRepository.findAllByOrderByIdUsuarioAsc());
        model.addAttribute("perfilesL", perfilRepository.findAllByOrderByNombreAsc());
        model.addAttribute("estadosUsuarioL", UsuarioService.ESTADOS);
        model.addAttribute("usuariosPendientes", usuarioRepository.countByEstado("pendiente"));
        model.addAttribute("rolesL", rolRepository.findAll());
        model.addAttribute("solicitudesL", solicitudes);
        model.addAttribute("pedidosL", pedidoRepository.findAllByOrderByFechaDesc());
        model.addAttribute("pedidosPendientes", pedidoRepository.countByEstado("pendiente"));
        model.addAttribute("pedidosPorEntregar", pedidoRepository.countByEstado("pagado"));
        model.addAttribute("metodosPagoL", VentaService.METODOS_PAGO);

        // ── Ventas y reporte por mes / año ───────────────────────────
        var ventas = ventaRepository.findAllByOrderByFechaDesc();
        model.addAttribute("ventasL", ventas);
        model.addAttribute("ventasPorPedido", ventas.stream()
                .collect(Collectors.toMap(v -> v.getPedido().getIdPedido(), v -> v)));
        List<Integer> anios = reporteVentasService.anios();
        int anioSel = anio != null ? anio : anios.get(0);
        List<ReporteMes> meses = reporteVentasService.porMes(anioSel);
        List<ReporteMes> porAnio = reporteVentasService.porAnio();
        model.addAttribute("aniosL", anios);
        model.addAttribute("anioSel", anioSel);
        model.addAttribute("reporteMeses", meses);
        model.addAttribute("reporteTotalAnio", ReporteVentasService.totalDe(meses, "Total " + anioSel));
        model.addAttribute("reporteAnios", porAnio);
        model.addAttribute("reporteTotalGeneral", ReporteVentasService.totalDe(porAnio, "Total general"));
        model.addAttribute("reporteMesesLabels", meses.stream().map(m -> m.nombreMes().substring(0, 3)).toList());
        model.addAttribute("reporteMesesTotales", meses.stream().map(m -> m.total().doubleValue()).toList());

        List<MovimientoFila> movimientos = reporteService.listarMovimientos();
        List<MovimientoFila> entradas = movimientos.stream().filter(MovimientoFila::esEntrada).collect(Collectors.toList());
        List<MovimientoFila> salidas = movimientos.stream().filter(m -> !m.esEntrada()).collect(Collectors.toList());
        model.addAttribute("movimientosL", movimientos);
        model.addAttribute("movTotal", movimientos.size());
        model.addAttribute("movEntradas", entradas.size());
        model.addAttribute("movSalidas", salidas.size());
        model.addAttribute("movUnidades", movimientos.stream().mapToInt(MovimientoFila::unidades).sum());
        model.addAttribute("movUnidadesEntrada", entradas.stream().mapToInt(MovimientoFila::unidades).sum());
        model.addAttribute("movUnidadesSalida", salidas.stream().mapToInt(MovimientoFila::unidades).sum());

        return "index";
    }

    private AlertaReposicion toAlerta(Producto p) {
        return new AlertaReposicion(p.getCodigo(), p.getNombre(), p.getTalla(), p.getColor(), p.getStock(),
                p.getStockMinimo(), p.getStock() == 0);
    }

    private SolicitudResumen toSolicitudResumen(com.trama.wms.model.Solicitud s) {
        return new SolicitudResumen(
                s.getIdSolicitud(),
                s.getCodigo(),
                s.getFecha() == null ? "—" : s.getFecha().toString(),
                s.getSucursal() != null ? s.getSucursal().getNombre() : "—",
                s.getSucursal() != null ? s.getSucursal().getIdSucursal() : null,
                s.getUsuario() != null ? s.getUsuario().getNombreCompleto() : "—",
                s.getUsuario() != null ? s.getUsuario().getIdUsuario() : null,
                s.getEstado(),
                s.getDetalles() == null ? 0 : s.getDetalles().size());
    }

    /**
     * Solicitud aplanada para la vista: se despejan las relaciones LAZY para no
     * depender de la sesion abierta al renderizar el index.
     */
    public record SolicitudResumen(Integer idSolicitud, String codigo, String fecha, String sucursal,
            Integer idSucursal, String usuario, Integer idUsuario, String estado, int items) {
    }

}
