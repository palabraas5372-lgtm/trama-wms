package com.trama.wms.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.dto.UsuarioForm;
import com.trama.wms.model.Perfil;
import com.trama.wms.model.Usuario;
import com.trama.wms.repository.MovimientoRepository;
import com.trama.wms.repository.PedidoRepository;
import com.trama.wms.repository.PerfilRepository;
import com.trama.wms.repository.RolRepository;
import com.trama.wms.repository.SolicitudRepository;
import com.trama.wms.repository.SucursalRepository;
import com.trama.wms.repository.UsuarioRepository;

/**
 * Reglas de negocio de los usuarios: registro publico desde el login y CRUD del
 * administrador. Aqui viven las dos validaciones pedidas:
 *   a. el DNI no puede repetirse;
 *   b. el nombre de usuario no puede repetirse;
 *   c. el telefono no puede repetirse;
 *   d. el correo no puede repetirse.
 */
@Service
public class UsuarioService {

    public static final List<String> ESTADOS = List.of(
            Usuario.ESTADO_ACTIVO, Usuario.ESTADO_PENDIENTE, Usuario.ESTADO_ANULADO);

    private static final Set<String> ESTADOS_VALIDOS = Set.copyOf(ESTADOS);

    private final UsuarioRepository repository;
    private final PerfilRepository perfilRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final MovimientoRepository movimientoRepository;
    private final SolicitudRepository solicitudRepository;
    private final PedidoRepository pedidoRepository;
    private final AuthService authService;

    public UsuarioService(UsuarioRepository repository, PerfilRepository perfilRepository,
            RolRepository rolRepository, SucursalRepository sucursalRepository,
            MovimientoRepository movimientoRepository, SolicitudRepository solicitudRepository,
            PedidoRepository pedidoRepository, AuthService authService) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
        this.rolRepository = rolRepository;
        this.sucursalRepository = sucursalRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudRepository = solicitudRepository;
        this.pedidoRepository = pedidoRepository;
        this.authService = authService;
    }

    /* ══════════════ Registro publico (enlace del login) ══════════════ */

    /**
     * Solo creacion. El estado SIEMPRE nace "pendiente": aunque alguien manipule
     * el formulario, solo un administrador puede activar la cuenta.
     */
    @Transactional
    public Usuario registrar(UsuarioForm form) {
        form.setIdUsuario(null);
        form.setEstado(Usuario.ESTADO_PENDIENTE);
        form.setRol(null);

        // En el registro el correo debe ser de Gmail
        String correo = form.getCorreo() == null ? "" : form.getCorreo().trim().toLowerCase();
        if (!correo.matches("[a-z0-9._%+-]+@gmail\\.com")) {
            throw new UsuarioInvalidoException("El correo debe ser una cuenta de Gmail (terminar en @gmail.com).");
        }

        // El almacén (sucursal) que el cliente contactará para sus pedidos es obligatorio
        if (form.getSucursal() == null || sucursalRepository.findById(form.getSucursal())
                .filter(s -> "activa".equalsIgnoreCase(s.getEstado())).isEmpty()) {
            throw new UsuarioInvalidoException("Selecciona el almacén con el que harás tus pedidos.");
        }
        return guardar(form);
    }

    /* ══════════════ CRUD del administrador ══════════════ */

    /** Guardar desde el CRUD: el administrador no puede quitarse a si mismo el acceso. */
    @Transactional
    public Usuario guardar(UsuarioForm form, Integer idSolicitante) {
        // La cuenta principal "admin" no puede perder su usuario, su perfil ni su estado activo,
        // aunque la edite otro administrador.
        if (form.getIdUsuario() != null && esPrincipal(form.getIdUsuario())) {
            // Solo el propio "admin" puede editar sus datos; los demás administradores no.
            if (!form.getIdUsuario().equals(idSolicitante)) {
                throw new UsuarioInvalidoException("Solo el usuario \"admin\" puede editar los datos de la cuenta principal.");
            }
            boolean sigueAdmin = form.getPerfil() != null && perfilRepository.findById(form.getPerfil())
                    .map(Perfil::esAdministrador).orElse(false);
            if (!Usuario.USUARIO_PRINCIPAL.equalsIgnoreCase(limpiar(form.getUsuario()))
                    || !sigueAdmin || !Usuario.ESTADO_ACTIVO.equals(form.getEstado())) {
                throw new UsuarioInvalidoException("La cuenta principal \"admin\" debe seguir siendo un "
                        + "administrador activo y no se puede renombrar.");
            }
        }
        if (form.getIdUsuario() != null && form.getIdUsuario().equals(idSolicitante)) {
            boolean sigueAdmin = form.getPerfil() != null && perfilRepository.findById(form.getPerfil())
                    .map(Perfil::esAdministrador).orElse(false);
            if (!sigueAdmin || !Usuario.ESTADO_ACTIVO.equals(form.getEstado())) {
                throw new UsuarioInvalidoException(
                        "No puedes quitarte el perfil de administrador ni desactivar tu propia cuenta.");
            }
        }
        return guardar(form);
    }

    @Transactional
    public Usuario guardar(UsuarioForm form) {

        boolean esNuevo = form.getIdUsuario() == null;

        String dni = limpiar(form.getDni());
        String usuario = limpiar(form.getUsuario());
        String nombres = limpiar(form.getNombres());
        String apellidos = limpiar(form.getApellidos());
        String telefono = limpiar(form.getTelefono()).replaceAll("[\\s-]", "");
        String correo = limpiar(form.getCorreo()).toLowerCase();
        String clave = form.getClave();

        // ── Campos obligatorios y formato ──
        if (!dni.matches("\\d{8}")) {
            throw new UsuarioInvalidoException("El DNI debe tener exactamente 8 dígitos.");
        }
        if (!telefono.matches("9\\d{8}")) {
            throw new UsuarioInvalidoException("El teléfono debe ser un celular de 9 dígitos que empiece con 9.");
        }
        if (!correo.matches("[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}")) {
            throw new UsuarioInvalidoException("Ingresa un correo electrónico válido.");
        }
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            throw new UsuarioInvalidoException("Ingresa los nombres y apellidos.");
        }
        if (!usuario.matches("[A-Za-z0-9._-]{4,40}")) {
            throw new UsuarioInvalidoException(
                    "El usuario debe tener entre 4 y 40 caracteres (letras, números, punto, guion).");
        }
        if (esNuevo && (clave == null || clave.length() < 6)) {
            throw new UsuarioInvalidoException("La clave debe tener al menos 6 caracteres.");
        }
        if (!esNuevo && clave != null && !clave.isBlank() && clave.length() < 6) {
            throw new UsuarioInvalidoException("La nueva clave debe tener al menos 6 caracteres.");
        }

        // ── a. Validacion por DNI ──
        boolean dniOcupado = esNuevo
                ? repository.existsByDni(dni)
                : repository.existsByDniAndIdUsuarioNot(dni, form.getIdUsuario());
        if (dniOcupado) {
            throw new UsuarioInvalidoException("El DNI " + dni + " ya está registrado.");
        }

        // ── b. Validacion por usuario ──
        boolean usuarioOcupado = esNuevo
                ? repository.existsByUsuarioIgnoreCase(usuario)
                : repository.existsByUsuarioIgnoreCaseAndIdUsuarioNot(usuario, form.getIdUsuario());
        if (usuarioOcupado) {
            throw new UsuarioInvalidoException("El usuario \"" + usuario + "\" ya está en uso. Elige otro.");
        }

        // ── c. Validacion por telefono ──
        boolean telefonoOcupado = esNuevo
                ? repository.existsByTelefono(telefono)
                : repository.existsByTelefonoAndIdUsuarioNot(telefono, form.getIdUsuario());
        if (telefonoOcupado) {
            throw new UsuarioInvalidoException("El teléfono " + telefono + " ya está registrado.");
        }

        // ── d. Validacion por correo ──
        boolean correoOcupado = esNuevo
                ? repository.existsByCorreoIgnoreCase(correo)
                : repository.existsByCorreoIgnoreCaseAndIdUsuarioNot(correo, form.getIdUsuario());
        if (correoOcupado) {
            throw new UsuarioInvalidoException("El correo " + correo + " ya está registrado.");
        }

        Perfil perfil = form.getPerfil() == null ? null : perfilRepository.findById(form.getPerfil()).orElse(null);
        if (perfil == null) {
            throw new UsuarioInvalidoException("Selecciona un perfil válido.");
        }

        String estado = form.getEstado() == null ? Usuario.ESTADO_PENDIENTE : form.getEstado().trim().toLowerCase();
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new UsuarioInvalidoException("Estado no válido: " + estado);
        }

        Usuario u = esNuevo ? new Usuario()
                : repository.findById(form.getIdUsuario())
                        .orElseThrow(() -> new UsuarioInvalidoException("El usuario a editar ya no existe."));

        u.setDni(dni);
        u.setTelefono(telefono);
        u.setCorreo(correo);
        u.setUsuario(usuario);
        u.setNombresApellidos(nombres, apellidos);
        u.setPerfil(perfil);
        u.setEstado(estado);
        u.setRol(form.getRol() == null ? null : rolRepository.findById(form.getRol()).orElse(null));
        u.setSucursal(form.getSucursal() == null ? null : sucursalRepository.findById(form.getSucursal()).orElse(null));

        // Al editar y dejar la clave en blanco se conserva el hash existente.
        if (clave != null && !clave.isBlank()) {
            u.setClave(authService.cifrar(clave));
        }

        return repository.save(u);
    }

    /** Accion rapida desde la tabla: activar o anular una cuenta. */
    @Transactional
    public Usuario cambiarEstado(Integer idUsuario, String estado, Integer idSolicitante) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new UsuarioInvalidoException("Estado no válido: " + estado);
        }
        if (idUsuario.equals(idSolicitante) && !Usuario.ESTADO_ACTIVO.equals(estado)) {
            throw new UsuarioInvalidoException("No puedes desactivar tu propia cuenta.");
        }
        Usuario u = repository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioInvalidoException("El usuario ya no existe."));
        if (u.esPrincipal() && !Usuario.ESTADO_ACTIVO.equals(estado)) {
            throw new UsuarioInvalidoException("La cuenta principal \"admin\" no se puede anular ni desactivar.");
        }
        u.setEstado(estado);
        return repository.save(u);
    }

    @Transactional
    public void eliminar(Integer idUsuario, Integer idSolicitante) {
        if (esPrincipal(idUsuario)) {
            throw new UsuarioInvalidoException("La cuenta principal \"admin\" no se puede eliminar.");
        }
        if (idUsuario.equals(idSolicitante)) {
            throw new UsuarioInvalidoException("No puedes eliminar tu propia cuenta.");
        }
        // Si tiene historial (movimientos, solicitudes o pedidos) no se borra: se anula.
        long historial = movimientoRepository.countByUsuario_IdUsuario(idUsuario)
                + solicitudRepository.countByUsuario_IdUsuario(idUsuario)
                + pedidoRepository.countByUsuario_IdUsuario(idUsuario);
        if (historial > 0) {
            throw new UsuarioInvalidoException(
                    "El usuario tiene pedidos, movimientos o solicitudes registradas; no se puede eliminar. "
                    + "Déjalo anulado para que ya no pueda ingresar.");
        }
        repository.deleteById(idUsuario);
    }

    /* ══════════════ Consultas para validar en vivo ══════════════ */

    public boolean dniDisponible(String dni) {
        return !repository.existsByDni(limpiar(dni));
    }

    public boolean usuarioDisponible(String usuario) {
        return !repository.existsByUsuarioIgnoreCase(limpiar(usuario));
    }

    public boolean telefonoDisponible(String telefono) {
        return !repository.existsByTelefono(limpiar(telefono).replaceAll("[\\s-]", ""));
    }

    public boolean correoDisponible(String correo) {
        return !repository.existsByCorreoIgnoreCase(limpiar(correo).toLowerCase());
    }

    private boolean esPrincipal(Integer idUsuario) {
        return repository.findById(idUsuario).map(Usuario::esPrincipal).orElse(false);
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
    }

}
