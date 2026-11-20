package com.trama.wms.dto;

import com.trama.wms.model.Perfil;
import com.trama.wms.model.Usuario;

/**
 * Datos del usuario autenticado que se guardan en la sesion HTTP.
 * Es una vista plana para no arrastrar la entidad JPA (y sus relaciones
 * perezosas) dentro de la sesion.
 */
public record UsuarioSesion(Integer idUsuario, String usuario, String nombreCompleto, String perfil,
        String rolNombre, String sucursalNombre) {

    public static UsuarioSesion de(Usuario u) {
        return new UsuarioSesion(
                u.getIdUsuario(),
                u.getUsuario(),
                u.getNombreCompleto(),
                u.getPerfil() != null ? u.getPerfil().getNombre() : Perfil.CLIENTE,
                u.getRol() != null ? u.getRol().getNombre() : "",
                u.getSucursal() != null ? u.getSucursal().getNombre() : "");
    }

    public boolean esAdministrador() {
        return Perfil.ADMINISTRADOR.equalsIgnoreCase(perfil);
    }

    /** Texto bajo el nombre en el sidebar: el cargo si lo tiene, si no el perfil. */
    public String etiqueta() {
        String p = perfil == null ? "" : perfil.substring(0, 1).toUpperCase() + perfil.substring(1);
        return rolNombre == null || rolNombre.isBlank() ? p : p + " · " + rolNombre;
    }

    /** Iniciales para el avatar, p. ej. "Julián Ramos" -> "JR". */
    public String iniciales() {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return usuario == null ? "?" : usuario.substring(0, 1).toUpperCase();
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
    }

}
