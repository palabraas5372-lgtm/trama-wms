package com.trama.wms.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.config.AuthInterceptor;
import com.trama.wms.dto.UsuarioSesion;
import com.trama.wms.model.Descuento;
import com.trama.wms.model.Pedido;
import com.trama.wms.model.Producto;
import com.trama.wms.model.Venta;
import com.trama.wms.repository.CategoriaRepository;
import com.trama.wms.repository.PedidoRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.UsuarioRepository;
import com.trama.wms.repository.VentaRepository;
import com.trama.wms.service.CalendarioService;
import com.trama.wms.service.ComprobantePdfService;
import com.trama.wms.service.PedidoInvalidoException;
import com.trama.wms.service.PedidoService;

import jakarta.servlet.http.HttpSession;

/**
 * Ambiente del perfil CLIENTE: catalogo, hacer pedidos, ver/cancelar sus
 * pedidos y datos de contacto de la empresa.
 */
@Controller
public class ClienteController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;
    private final VentaRepository ventaRepository;
    private final ComprobantePdfService pdfService;
    private final CalendarioService calendario;

    // Datos de contacto: se cambian en application.properties
    @Value("${trama.contacto.telefono:01-441-2200}")
    private String contactoTelefono;
    @Value("${trama.contacto.whatsapp:51999999999}")
    private String contactoWhatsapp;
    @Value("${trama.contacto.email:ventas@trama.pe}")
    private String contactoEmail;
    @Value("${trama.contacto.horario:Lunes a s\u00e1bado, 9:00 a 18:00}")
    private String contactoHorario;

    public ClienteController(ProductoRepository productoRepository, CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository, PedidoService pedidoService,
            VentaRepository ventaRepository, ComprobantePdfService pdfService, CalendarioService calendario) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
        this.ventaRepository = ventaRepository;
        this.pdfService = pdfService;
        this.calendario = calendario;
    }

    @GetMapping("/cliente")
    public String portal(@RequestParam(name = "categoria", required = false) Integer idCategoria,
            @RequestParam(name = "q", required = false) String busqueda,
            HttpSession session, Model model) {

        UsuarioSesion actual = sesion(session);

        List<Producto> productos = productoRepository.buscarCatalogo(idCategoria,
                busqueda == null || busqueda.isBlank() ? null : busqueda.trim().toLowerCase());
        List<Pedido> misPedidos = pedidoRepository.findByUsuario_IdUsuarioOrderByFechaDesc(actual.idUsuario());

        model.addAttribute("productosL", productos);
        // Para el modal de pedido: solo productos con stock (lista cargada desde la BD)
        // Solo se pueden pedir productos con al menos el mínimo de venta al por mayor
        model.addAttribute("pedibles", productoRepository.findAll().stream()
                .filter(p -> p.getStock() >= Pedido.MINIMO_POR_PRODUCTO).toList());
        model.addAttribute("minimoPedido", Pedido.MINIMO_POR_PRODUCTO);
        model.addAttribute("categoriasL", categoriaRepository.findAll());
        model.addAttribute("categoriaSel", idCategoria);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("disponibles", productos.stream().filter(p -> p.getStock() > 0).count());
        model.addAttribute("yo", usuarioRepository.findById(actual.idUsuario()).orElse(null));
        model.addAttribute("misPedidos", misPedidos);
        // Venta (comprobante) de cada pedido ya cobrado
        Map<Integer, Venta> ventasPorPedido = ventaRepository.findByCliente_IdUsuarioOrderByFechaDesc(actual.idUsuario())
                .stream().collect(Collectors.toMap(v -> v.getPedido().getIdPedido(), v -> v));
        model.addAttribute("ventasPorPedido", ventasPorPedido);
        // Descuentos por volumen (venta al por mayor)
        model.addAttribute("nivelesDescuento", Descuento.NIVELES);
        model.addAttribute("descMinimos", Descuento.NIVELES.stream().map(Descuento.Nivel::minimoUnidades).toList());
        model.addAttribute("descPorcentajes", Descuento.NIVELES.stream().map(Descuento.Nivel::porcentaje).toList());
        // Fechas especiales del Perú (descuento adicional)
        Descuento.Festivo festivo = calendario.festivoVigente();
        model.addAttribute("festivo", festivo);
        model.addAttribute("festivoPct", festivo == null ? 0 : festivo.porcentaje());
        model.addAttribute("proximoFestivo", calendario.proximoFestivo());
        model.addAttribute("festivosL", Descuento.festivos(calendario.hoy().getYear()));
        model.addAttribute("fechaSimulada", calendario.esSimulada() ? calendario.hoy() : null);
        model.addAttribute("pedidosPendientes",
                misPedidos.stream().filter(p -> Pedido.PENDIENTE.equals(p.getEstado())).count());

        model.addAttribute("contactoTelefono", contactoTelefono);
        model.addAttribute("contactoWhatsapp", contactoWhatsapp);
        model.addAttribute("contactoEmail", contactoEmail);
        model.addAttribute("contactoHorario", contactoHorario);
        return "cliente";
    }

    @PostMapping("/cliente/pedidos")
    public String crearPedido(@RequestParam(name = "productoId", required = false) List<Integer> productos,
            @RequestParam(name = "cantidad", required = false) List<Integer> cantidades,
            @RequestParam(name = "telefono", required = false) String telefono,
            @RequestParam(name = "observacion", required = false) String observacion,
            HttpSession session, RedirectAttributes atributos) {
        try {
            Pedido p = pedidoService.crear(sesion(session).idUsuario(), productos, cantidades, telefono, observacion);
            String descuento = "";
            if (p.getDescuentoVolumenPct() > 0) {
                descuento += " Descuento por volumen: " + p.getDescuentoVolumenPct() + "%.";
            }
            if (p.getDescuentoFestivoPct() > 0) {
                descuento += " Descuento " + p.getFestivoNombre() + ": " + p.getDescuentoFestivoPct() + "%.";
            }
            aviso(atributos, "success", "¡Pedido " + p.getCodigo() + " enviado!" + descuento
                    + " Te contactaremos al " + p.getTelefono() + ".");
        } catch (PedidoInvalidoException error) {
            aviso(atributos, "danger", error.getMessage());
        }
        return "redirect:/cliente#mis-pedidos";
    }

    @GetMapping("/cliente/pedidos/{id}/cancelar")
    public String cancelarPedido(@PathVariable Integer id, HttpSession session, RedirectAttributes atributos) {
        try {
            Pedido p = pedidoService.cancelarPorCliente(id, sesion(session).idUsuario());
            aviso(atributos, "success", "Pedido " + p.getCodigo() + " cancelado.");
        } catch (PedidoInvalidoException error) {
            aviso(atributos, "danger", error.getMessage());
        }
        return "redirect:/cliente#mis-pedidos";
    }

    /** El cliente descarga el comprobante (boleta o factura) de SUS pedidos cobrados. */
    @GetMapping("/cliente/comprobante/{idVenta}")
    public ResponseEntity<byte[]> comprobante(@PathVariable Integer idVenta, HttpSession session) {
        Venta v = pdfService.buscar(idVenta);
        if (!v.getCliente().getIdUsuario().equals(sesion(session).idUsuario())) {
            return ResponseEntity.notFound().build();
        }
        return VentaController.respuestaPdf(v, pdfService.generar(idVenta));
    }

    private UsuarioSesion sesion(HttpSession session) {
        return (UsuarioSesion) session.getAttribute(AuthInterceptor.SESION_USUARIO);
    }

    private void aviso(RedirectAttributes atributos, String tipo, String mensaje) {
        atributos.addFlashAttribute("mensaje", mensaje);
        atributos.addFlashAttribute("tipoMensaje", tipo);
    }

}
