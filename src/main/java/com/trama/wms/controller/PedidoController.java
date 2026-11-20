package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.config.AuthInterceptor;
import com.trama.wms.dto.CobroForm;
import com.trama.wms.dto.UsuarioSesion;
import com.trama.wms.model.Pedido;
import com.trama.wms.model.Venta;
import com.trama.wms.service.PedidoInvalidoException;
import com.trama.wms.service.PedidoService;
import com.trama.wms.service.VentaService;

import jakarta.servlet.http.HttpSession;

/**
 * Lado ADMINISTRADOR de los pedidos de clientes (el listado lo carga AppController).
 * Flujo: pendiente → (cobrar) pagado → (entregar) entregado. Un pendiente se puede cancelar.
 */
@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final VentaService ventaService;

    public PedidoController(PedidoService pedidoService, VentaService ventaService) {
        this.pedidoService = pedidoService;
        this.ventaService = ventaService;
    }

    /** Cobrar: registra el pago, genera la venta con su comprobante y descuenta el stock. */
    @PostMapping("/{id}/cobrar")
    public String cobrar(@PathVariable Integer id, @ModelAttribute CobroForm form, HttpSession session,
            RedirectAttributes atributos) {
        UsuarioSesion admin = (UsuarioSesion) session.getAttribute(AuthInterceptor.SESION_USUARIO);
        try {
            Venta v = ventaService.cobrar(id, admin.idUsuario(), form);
            aviso(atributos, "success", "Pedido cobrado: se emitió la " + (v.esFactura() ? "factura " : "boleta ")
                    + v.getComprobante() + " por S/ " + v.getTotal() + ".");
            atributos.addFlashAttribute("ventaRecienCobrada", v.getIdVenta());
        } catch (PedidoInvalidoException error) {
            aviso(atributos, "danger", error.getMessage());
        }
        return "redirect:/index#pedidos";
    }

    @GetMapping("/{id}/entregar")
    public String entregar(@PathVariable Integer id, RedirectAttributes atributos) {
        try {
            Pedido p = ventaService.entregar(id);
            aviso(atributos, "success", "Pedido " + p.getCodigo() + " marcado como ENTREGADO.");
        } catch (PedidoInvalidoException error) {
            aviso(atributos, "danger", error.getMessage());
        }
        return "redirect:/index#pedidos";
    }

    @GetMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, RedirectAttributes atributos) {
        try {
            Pedido p = pedidoService.cancelarPorAdministrador(id);
            aviso(atributos, "success", "Pedido " + p.getCodigo() + " cancelado.");
        } catch (PedidoInvalidoException error) {
            aviso(atributos, "danger", error.getMessage());
        }
        return "redirect:/index#pedidos";
    }

    private void aviso(RedirectAttributes atributos, String tipo, String mensaje) {
        atributos.addFlashAttribute("mensaje", mensaje);
        atributos.addFlashAttribute("tipoMensaje", tipo);
    }

}
