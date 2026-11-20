package com.trama.wms.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.trama.wms.config.AuthInterceptor;
import com.trama.wms.dto.UsuarioSesion;
import com.trama.wms.model.Usuario;
import com.trama.wms.service.AuthService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /** Raiz del sistema: si ya hay sesion activa entra directo a su ambiente. */
    @GetMapping("/")
    public String inicio(HttpSession session) {
        UsuarioSesion actual = (UsuarioSesion) session.getAttribute(AuthInterceptor.SESION_USUARIO);
        if (actual != null) {
            return destino(actual);
        }
        return "login";
    }

    /**
     * 1. Valida usuario y clave contra la tabla usuarios.
     * 2. Revisa el estado de la cuenta (solo "activo" puede entrar).
     * 3. Segun el perfil, redirige al ambiente de administrador o de cliente.
     */
    @PostMapping("/login")
    public String login(@RequestParam("usuario") String usuario, @RequestParam("clave") String clave,
            HttpSession session, Model model) {

        model.addAttribute("usuarioIntentado", usuario);

        Optional<Usuario> encontrado = authService.autenticar(usuario, clave);

        if (encontrado.isEmpty()) {
            model.addAttribute("error", "Usuario o contraseña incorrectos. Intenta nuevamente.");
            return "login";
        }

        Usuario u = encontrado.get();

        if (Usuario.ESTADO_PENDIENTE.equalsIgnoreCase(u.getEstado())) {
            model.addAttribute("error",
                    "Tu cuenta está pendiente de activación. Un administrador debe aprobarla antes de ingresar.");
            return "login";
        }
        if (!Usuario.ESTADO_ACTIVO.equalsIgnoreCase(u.getEstado())) {
            model.addAttribute("error", "Tu cuenta fue anulada. Comunícate con el administrador.");
            return "login";
        }

        UsuarioSesion sesion = UsuarioSesion.de(u);
        session.setAttribute(AuthInterceptor.SESION_USUARIO, sesion);
        session.setMaxInactiveInterval(60 * 60);

        return destino(sesion);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    /** Ambiente segun el perfil: administrador -> panel del almacen; cliente -> portal. */
    private String destino(UsuarioSesion sesion) {
        return sesion.esAdministrador() ? "redirect:/index" : "redirect:/cliente";
    }

}
