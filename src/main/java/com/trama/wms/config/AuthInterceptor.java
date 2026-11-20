package com.trama.wms.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.trama.wms.dto.UsuarioSesion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege todas las rutas del sistema:
 *  - sin sesion -> al login;
 *  - perfil cliente -> solo puede entrar a /cliente/**; cualquier ruta del
 *    administrador (index, CRUDs) lo devuelve a su portal.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESION_USUARIO = "usuarioSesion";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        UsuarioSesion usuario = (UsuarioSesion) request.getSession().getAttribute(SESION_USUARIO);
        String contexto = request.getContextPath();

        if (usuario == null) {
            response.sendRedirect(contexto + "/");
            return false;
        }

        String ruta = request.getRequestURI().substring(contexto.length());
        boolean rutaCliente = ruta.equals("/cliente") || ruta.startsWith("/cliente/");

        if (!usuario.esAdministrador() && !rutaCliente) {
            response.sendRedirect(contexto + "/cliente");
            return false;
        }

        return true;
    }

}
