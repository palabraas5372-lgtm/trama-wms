package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.dto.MovimientoForm;
import com.trama.wms.service.MovimientoInvalidoException;
import com.trama.wms.service.MovimientoService;

/**
 * Mutaciones del modulo de entradas y salidas: el historial y los combos los carga
 * AppController en index.html. El detalle (producto y cantidad) lo recibe en
 * MovimientoForm y MovimientoService se encarga de ajustar el stock.
 */
@Controller
@RequestMapping("/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute MovimientoForm form, RedirectAttributes atributos) {
        try {
            movimientoService.registrar(form);
            atributos.addFlashAttribute("mensaje", form.getIdMovimiento() == null
                    ? "Movimiento registrado y stock actualizado."
                    : "Movimiento actualizado y stock ajustado.");
            atributos.addFlashAttribute("tipoMensaje", "success");
        } catch (MovimientoInvalidoException error) {
            atributos.addFlashAttribute("mensaje", error.getMessage());
            atributos.addFlashAttribute("tipoMensaje", error.esStockInsuficiente() ? "warning" : "danger");
        }
        return "redirect:/index#movimientos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes atributos) {
        movimientoService.eliminar(id);
        atributos.addFlashAttribute("mensaje", "Movimiento eliminado y stock devuelto al inventario.");
        atributos.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/index#movimientos";
    }

}
