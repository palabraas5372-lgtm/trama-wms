package com.trama.wms.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.trama.wms.dto.UsuarioSesion;

import jakarta.servlet.http.HttpSession;

/**
 * Publica el usuario de la sesion en todas las vistas, para que el sidebar
 * y el topbar puedan mostrarlo sin que cada controller lo repita.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("usuarioActual")
    public UsuarioSesion usuarioActual(HttpSession session) {
        return (UsuarioSesion) session.getAttribute(AuthInterceptor.SESION_USUARIO);
    }

}
