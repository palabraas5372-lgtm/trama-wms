package com.trama.wms.model;

import jakarta.persistence.*;

/**
 * Perfil de acceso al sistema. Define a que ambiente entra el usuario al
 * iniciar sesion: "administrador" (panel completo del almacen) o "cliente"
 * (portal del cliente). Los valores se cargan desde la tabla perfiles para
 * poblar las listas desplegables del registro y del CRUD de usuarios.
 */
@Entity
@Table(name = "perfiles")
public class Perfil {

    public static final String ADMINISTRADOR = "administrador";
    public static final String CLIENTE = "cliente";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Integer idPerfil;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "descripcion", length = 120)
    private String descripcion;

    public Perfil() {}

    public Perfil(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public boolean esAdministrador() {
        return ADMINISTRADOR.equalsIgnoreCase(nombre);
    }

    public Integer getIdPerfil() {
        return idPerfil;
    }

    public void setIdPerfil(Integer idPerfil) {
        this.idPerfil = idPerfil;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
