package com.trama.wms.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.config.AuthInterceptor;
import com.trama.wms.dto.UsuarioForm;
import com.trama.wms.dto.UsuarioSesion;
import com.trama.wms.model.Usuario;
import com.trama.wms.service.UsuarioInvalidoException;
import com.trama.wms.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

/**
 * CRUD de usuarios del perfil administrador (el listado lo carga AppController
 * en index.html). Solo un administrador llega aqui: AuthInterceptor redirige a
 * los clientes a su portal.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final String VOLVER = "redirect:/index#usuarios";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute UsuarioForm form, HttpSession session, RedirectAttributes atributos) {
        try {
            Usuario u = usuarioService.guardar(form, idActual(session));
            ok(atributos, form.getIdUsuario() == null
                    ? "Usuario " + u.getUsuario() + " registrado."
                    : "Usuario " + u.getUsuario() + " actualizado.");
        } catch (UsuarioInvalidoException error) {
            fallo(atributos, error.getMessage());
        }
        return VOLVER;
    }

    @GetMapping("/activar/{id}")
    public String activar(@PathVariable Integer id, HttpSession session, RedirectAttributes atributos) {
        return cambiarEstado(id, Usuario.ESTADO_ACTIVO, session, atributos);
    }

    @GetMapping("/anular/{id}")
    public String anular(@PathVariable Integer id, HttpSession session, RedirectAttributes atributos) {
        return cambiarEstado(id, Usuario.ESTADO_ANULADO, session, atributos);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes atributos) {
        try {
            usuarioService.eliminar(id, idActual(session));
            ok(atributos, "Usuario eliminado.");
        } catch (UsuarioInvalidoException error) {
            fallo(atributos, error.getMessage());
        } catch (DataIntegrityViolationException error) {
            // Cualquier otro registro que dependa del usuario: mensaje claro en vez de la página de error
            fallo(atributos, "No se puede eliminar: el usuario tiene registros asociados. Déjalo anulado.");
        }
        return VOLVER;
    }

    private String cambiarEstado(Integer id, String estado, HttpSession session, RedirectAttributes atributos) {
        try {
            Usuario u = usuarioService.cambiarEstado(id, estado, idActual(session));
            ok(atributos, "Usuario " + u.getUsuario() + " ahora está " + estado + ".");
        } catch (UsuarioInvalidoException error) {
            fallo(atributos, error.getMessage());
        }
        return VOLVER;
    }

    private Integer idActual(HttpSession session) {
        return ((UsuarioSesion) session.getAttribute(AuthInterceptor.SESION_USUARIO)).idUsuario();
    }

    private void ok(RedirectAttributes atributos, String mensaje) {
        atributos.addFlashAttribute("mensaje", mensaje);
        atributos.addFlashAttribute("tipoMensaje", "success");
    }

    private void fallo(RedirectAttributes atributos, String mensaje) {
        atributos.addFlashAttribute("mensaje", mensaje);
        atributos.addFlashAttribute("tipoMensaje", "danger");
    }
}
