package com.trama.wms.dto;

/**
 * Datos que llegan desde el formulario de registro (login) o desde el modal del
 * CRUD de usuarios. Se usa un DTO para no enlazar directamente la entidad JPA.
 */
public class UsuarioForm {

    private Integer idUsuario;
    private String dni;
    private String telefono;
    private String correo;
    private String nombres;
    private String apellidos;
    private String usuario;
    private String clave;
    private Integer perfil;
    private String estado;
    private Integer rol;
    private Integer sucursal;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public Integer getPerfil() { return perfil; }
    public void setPerfil(Integer perfil) { this.perfil = perfil; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getRol() { return rol; }
    public void setRol(Integer rol) { this.rol = rol; }

    public Integer getSucursal() { return sucursal; }
    public void setSucursal(Integer sucursal) { this.sucursal = sucursal; }
}
