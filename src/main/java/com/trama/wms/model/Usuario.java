package com.trama.wms.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    /* Estados posibles de una cuenta */
    public static final String ESTADO_ACTIVO = "activo";
    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_ANULADO = "anulado";

    /** Cuenta principal del sistema (Julián Ramos): nadie puede borrarla, anularla ni quitarle el perfil. */
    public static final String USUARIO_PRINCIPAL = "admin";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;
    
    /** Perfil de acceso (administrador / cliente), cargado desde la tabla perfiles. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_perfil")
    private Perfil perfil;

    /* Rol (cargo dentro del almacen) y sucursal son opcionales: un cliente que se
       registra desde el login no los tiene; el administrador puede asignarlos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol")
    private Rol rol;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @Column(name = "dni", unique = true, length = 8)
    private String dni;

    @Column(name = "telefono", unique = true, length = 9)
    private String telefono;

    @Column(name = "correo", unique = true, length = 100)
    private String correo;

    @Column(name = "nombres", length = 60)
    private String nombres;

    @Column(name = "apellidos", length = 60)
    private String apellidos;
    
    @Column(name = "usuario", nullable = false, unique = true, length = 40)
    private String usuario;
    
    @Column(name = "clave", nullable = false, length = 255)
    private String clave;
    
    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;
    
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = ESTADO_PENDIENTE;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Movimiento> movimientos;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Solicitud> solicitudes;
    
    public Usuario() {}
    
    public Integer getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean esPrincipal() {
        return USUARIO_PRINCIPAL.equalsIgnoreCase(usuario);
    }

    public boolean esAdministrador() {
        return perfil != null && perfil.esAdministrador();
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    /**
     * Asigna nombres y apellidos y recalcula nombre_completo, que el resto del
     * sistema (movimientos, solicitudes, sidebar) sigue usando para mostrar.
     */
    public void setNombresApellidos(String nombres, String apellidos) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.nombreCompleto = (nombres + " " + apellidos).trim();
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public Rol getRol() {
        return rol;
    }
    
    public void setRol(Rol rol) {
        this.rol = rol;
    }
    
    public Sucursal getSucursal() {
        return sucursal;
    }
    
    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }
    
    public String getUsuario() {
        return usuario;
    }
    
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
    
    public String getClave() {
        return clave;
    }
    
    public void setClave(String clave) {
        this.clave = clave;
    }
    
    public String getNombreCompleto() {
        return nombreCompleto;
    }
    
    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public List<Movimiento> getMovimientos() {
        return movimientos;
    }
    
    public void setMovimientos(List<Movimiento> movimientos) {
        this.movimientos = movimientos;
    }
    
    public List<Solicitud> getSolicitudes() {
        return solicitudes;
    }
    
    public void setSolicitudes(List<Solicitud> solicitudes) {
        this.solicitudes = solicitudes;
    }
}