package com.trama.wms.init;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import com.trama.wms.model.Categoria;
import com.trama.wms.model.DetalleMovimiento;
import com.trama.wms.model.DetalleSolicitud;
import com.trama.wms.model.Movimiento;
import com.trama.wms.model.Perfil;
import com.trama.wms.model.Producto;
import com.trama.wms.model.Proveedor;
import com.trama.wms.model.Rol;
import com.trama.wms.model.Solicitud;
import com.trama.wms.model.Sucursal;
import com.trama.wms.model.Usuario;
import com.trama.wms.repository.CategoriaRepository;
import com.trama.wms.repository.MovimientoRepository;
import com.trama.wms.repository.PerfilRepository;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.repository.ProveedorRepository;
import com.trama.wms.repository.RolRepository;
import com.trama.wms.repository.SolicitudRepository;
import com.trama.wms.repository.SucursalRepository;
import com.trama.wms.repository.UsuarioRepository;
import com.trama.wms.service.UsuarioService;

/**
 * Carga los datos de prueba del sistema TRAMA WMS la primera vez que se levanta
 * la aplicacion. Solo se ejecuta cuando la tabla de roles esta vacia, de modo que
 * nunca pisa informacion registrada por el usuario.
 *
 * Credenciales de acceso: admin / trama2026
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String CLAVE_ADMIN = "trama2026";

    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final SolicitudRepository solicitudRepository;
    private final PerfilRepository perfilRepository;

    private final DataSource dataSource;

    public DataInitializer(RolRepository rolRepository, SucursalRepository sucursalRepository,
            UsuarioRepository usuarioRepository, ProveedorRepository proveedorRepository,
            CategoriaRepository categoriaRepository, ProductoRepository productoRepository,
            MovimientoRepository movimientoRepository, SolicitudRepository solicitudRepository,
            PerfilRepository perfilRepository, DataSource dataSource) {
        this.rolRepository = rolRepository;
        this.sucursalRepository = sucursalRepository;
        this.usuarioRepository = usuarioRepository;
        this.proveedorRepository = proveedorRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudRepository = solicitudRepository;
        this.perfilRepository = perfilRepository;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(String... args) {

        relajarColumnasOpcionales();

        Perfil perfilAdmin = asegurarPerfil(Perfil.ADMINISTRADOR, "Acceso al panel completo del almacén");
        Perfil perfilCliente = asegurarPerfil(Perfil.CLIENTE, "Acceso al portal de clientes");

        if (rolRepository.count() > 0) {
            completarUsuariosAntiguos(perfilAdmin, perfilCliente);
            completarPrecios();
            log.info("Datos de prueba ya cargados: se omite el inicializador.");
            return;
        }

        log.info("Cargando datos de prueba de TRAMA WMS...");

        Rol jefe = guardarRol("Jefe de Almacén");
        Rol operador = guardarRol("Operador");
        Rol encargadoRol = guardarRol("Encargado de Sucursal");

        Sucursal miraflores = guardarSucursal("TRAMA Miraflores", "Lima", "Rocío Salas", "01-441-2200", "activa");
        Sucursal sanIsidro = guardarSucursal("TRAMA San Isidro", "Lima", "Diego Ponte", "01-221-0099", "activa");
        Sucursal trujillo = guardarSucursal("TRAMA Trujillo", "Trujillo", "Ana Chávez", "044-500-321", "activa");
        guardarSucursal("TRAMA Arequipa", "Arequipa", "Iván Torres", "054-330-118", "en revisión");

        Usuario admin = new Usuario();
        admin.setRol(jefe);
        admin.setSucursal(miraflores);
        admin.setUsuario("admin");
        admin.setClave(new BCryptPasswordEncoder().encode(CLAVE_ADMIN));
        admin.setDni("40112233");
        admin.setTelefono("987000001");
        admin.setCorreo("admin@trama.pe");
        admin.setNombresApellidos("Julián", "Ramos");
        admin.setPerfil(perfilAdmin);
        admin.setEstado("activo");
        admin.setMovimientos(new ArrayList<>());
        admin.setSolicitudes(new ArrayList<>());
        usuarioRepository.save(admin);

        Usuario supervisor = new Usuario();
        supervisor.setRol(operador);
        supervisor.setSucursal(sanIsidro);
        supervisor.setUsuario("operador");
        supervisor.setClave(new BCryptPasswordEncoder().encode(CLAVE_ADMIN));
        supervisor.setDni("45667788");
        supervisor.setTelefono("987000002");
        supervisor.setCorreo("operador@trama.pe");
        supervisor.setNombresApellidos("Diego", "Contreras");
        supervisor.setPerfil(perfilAdmin);
        supervisor.setEstado("activo");
        supervisor.setMovimientos(new ArrayList<>());
        supervisor.setSolicitudes(new ArrayList<>());
        usuarioRepository.save(supervisor);

        Usuario encargado = new Usuario();
        encargado.setRol(encargadoRol);
        encargado.setSucursal(trujillo);
        encargado.setUsuario("encargado");
        encargado.setClave(new BCryptPasswordEncoder().encode(CLAVE_ADMIN));
        encargado.setDni("41998877");
        encargado.setTelefono("987000003");
        encargado.setCorreo("encargado@trama.pe");
        encargado.setNombresApellidos("Ana", "Chávez");
        encargado.setPerfil(perfilAdmin);
        encargado.setEstado("activo");
        encargado.setMovimientos(new ArrayList<>());
        encargado.setSolicitudes(new ArrayList<>());
        usuarioRepository.save(encargado);

        // Cliente activo de ejemplo y uno pendiente (para probar la activacion)
        guardarCliente(perfilCliente, miraflores, "70123456", "987654321", "maria.quispe@gmail.com", "María", "Quispe Rojas",
                "cliente", Usuario.ESTADO_ACTIVO);
        guardarCliente(perfilCliente, sanIsidro, "72345678", "912345678", "luis.paredes@gmail.com", "Luis", "Paredes Soto",
                "lparedes", Usuario.ESTADO_PENDIENTE);

        Proveedor andina = guardarProveedor("Proveedor Textil Andina", "Mario Reyes", "01-500-1111",
                "ventas@textilandina.pe");
        Proveedor sur = guardarProveedor("Proveedor Confecciones Sur", "Lucía Prado", "054-600-2222",
                "contacto@confeccionessur.pe");

        Categoria camisas = guardarCategoria("Camisas", "Camisas de vestir en algodón y popel");
        Categoria polos = guardarCategoria("Polos", "Polos piqué para temporada");
        Categoria pantalones = guardarCategoria("Pantalones", "Pantalones de mezclilla y vestir");
        Categoria chompas = guardarCategoria("Chompas", "Chompas de punto y polar");
        Categoria casacas = guardarCategoria("Casacas", "Casacas cortavientos y polar");
        Categoria faldas = guardarCategoria("Faldas", "Faldas plisadas y tubulares");
        Categoria vestidos = guardarCategoria("Vestidos", "Vestidos de ocasión");

        Producto oxfordAzul = guardarProducto("TR-1001", "Camisa Oxford Clásica", camisas, "M", "Azul", "#2F4771",
                120, 40, "A-01-03");
        Producto oxfordBlanco = guardarProducto("TR-1002", "Camisa Oxford Clásica", camisas, "L", "Blanco", "#F4F2EC",
                18, 40, "A-01-04");
        Producto poloNegro = guardarProducto("TR-1010", "Polo Piqué Básico", polos, "S", "Negro", "#1B1F2A",
                200, 50, "A-02-01");
        Producto poloMostaza = guardarProducto("TR-1011", "Polo Piqué Básico", polos, "M", "Mostaza", "#E8A33D",
                6, 50, "A-02-02");
        guardarProducto("TR-1020", "Jean Slim Fit", pantalones, "32", "Denim Oscuro", "#223554", 75, 30, "B-01-01");
        Producto jeanClaro = guardarProducto("TR-1021", "Jean Slim Fit", pantalones, "34", "Denim Claro", "#7C93B8",
                0, 30, "B-01-02");
        Producto chompaGris = guardarProducto("TR-1030", "Chompa Cuello Redondo", chompas, "M", "Gris", "#9CA3AF",
                54, 25, "B-02-05");
        Producto cortavientos = guardarProducto("TR-1040", "Casaca Cortavientos", casacas, "L", "Verde Olivo", "#5B6B4C",
                33, 20, "C-01-01");
        guardarProducto("TR-1050", "Falda Plisada", faldas, "S", "Terracota", "#C1483B", 41, 15, "C-02-03");
        Producto vestidoVino = guardarProducto("TR-1060", "Vestido Manga Larga", vestidos, "M", "Vino", "#7A2E38",
                22, 20, "C-03-01");

        // Recepcion de mercaderia del proveedor central
        Movimiento entrada = new Movimiento();
        entrada.setSucursal(null);
        entrada.setProveedor(andina);
        entrada.setUsuario(admin);
        entrada.setTipo("entrada");
        entrada.setFecha(LocalDateTime.now().minusDays(1).withSecond(0).withNano(0));
        entrada.setObservacion("Recepción de mercadería del proveedor");
        entrada.setDetalles(new ArrayList<>());
        agregarDetalleMovimiento(entrada, oxfordAzul, 60);
        agregarDetalleMovimiento(entrada, cortavientos, 33);
        movimientoRepository.save(entrada);

        // Despacho a la sucursal de Miraflores
        Movimiento salida = new Movimiento();
        salida.setSucursal(miraflores);
        salida.setProveedor(null);
        salida.setUsuario(admin);
        salida.setTipo("salida");
        salida.setFecha(LocalDateTime.now().minusDays(1).withHour(11).withMinute(40).withSecond(0).withNano(0));
        salida.setObservacion("Despacho a sucursal Miraflores");
        salida.setDetalles(new ArrayList<>());
        agregarDetalleMovimiento(salida, poloMostaza, 30);
        movimientoRepository.save(salida);

        // Entrada received from the sur supplier for San Isidro
        Movimiento entradaSur = new Movimiento();
        entradaSur.setSucursal(sanIsidro);
        entradaSur.setProveedor(sur);
        entradaSur.setUsuario(supervisor);
        entradaSur.setTipo("entrada");
        entradaSur.setFecha(LocalDateTime.now().minusDays(3).withHour(8).withMinute(15).withSecond(0).withNano(0));
        entradaSur.setObservacion("Transferencia entre sucursales");
        entradaSur.setDetalles(new ArrayList<>());
        agregarDetalleMovimiento(entradaSur, oxfordBlanco, 25);
        agregarDetalleMovimiento(entradaSur, jeanClaro, 12);
        movimientoRepository.save(entradaSur);

        // Salida hacia la sucursal de Trujillo
        Movimiento salidaTrujillo = new Movimiento();
        salidaTrujillo.setSucursal(trujillo);
        salidaTrujillo.setProveedor(null);
        salidaTrujillo.setUsuario(encargado);
        salidaTrujillo.setTipo("salida");
        salidaTrujillo.setFecha(LocalDateTime.now().minusDays(5).withHour(15).withMinute(5).withSecond(0).withNano(0));
        salidaTrujillo.setObservacion("Despacho por reposición de stock");
        salidaTrujillo.setDetalles(new ArrayList<>());
        agregarDetalleMovimiento(salidaTrujillo, poloNegro, 80);
        agregarDetalleMovimiento(salidaTrujillo, chompaGris, 20);
        movimientoRepository.save(salidaTrujillo);

        Solicitud pendiente = new Solicitud();
        pendiente.setCodigo("SOL-3001");
        pendiente.setSucursal(miraflores);
        pendiente.setUsuario(admin);
        pendiente.setFecha(LocalDate.now().minusDays(1));
        pendiente.setEstado("pendiente");
        pendiente.setDetalles(new ArrayList<>());
        agregarDetalleSolicitud(pendiente, poloMostaza, 40);
        agregarDetalleSolicitud(pendiente, jeanClaro, 20);
        solicitudRepository.save(pendiente);

        Solicitud transito = new Solicitud();
        transito.setCodigo("SOL-3002");
        transito.setSucursal(trujillo);
        transito.setUsuario(encargado);
        transito.setFecha(LocalDate.now().minusDays(4));
        transito.setEstado("en tránsito");
        transito.setDetalles(new ArrayList<>());
        agregarDetalleSolicitud(transito, vestidoVino, 15);
        solicitudRepository.save(transito);

        log.info("Datos de prueba cargados. Acceso: admin / {} · cliente / {}", CLAVE_ADMIN, CLAVE_ADMIN);
    }

    /* ══════════════ Precios de venta al por mayor (trabajo final) ══════════════ */

    /** Precio por unidad (soles) según la categoría, para los datos de prueba. */
    private static java.math.BigDecimal precioSugerido(String categoria) {
        int precio = switch (categoria == null ? "" : categoria) {
            case "Camisas" -> 45;
            case "Polos" -> 25;
            case "Pantalones" -> 60;
            case "Chompas" -> 55;
            case "Casacas" -> 85;
            case "Faldas" -> 40;
            case "Vestidos" -> 70;
            default -> 30;
        };
        return java.math.BigDecimal.valueOf(precio).setScale(2);
    }

    /** BD creada antes de existir la columna precio: se asigna un precio inicial. */
    private void completarPrecios() {
        for (Producto p : productoRepository.findAll()) {
            if (p.getPrecio().signum() == 0) {
                p.setPrecio(precioSugerido(p.getCategoria() != null ? p.getCategoria().getNombre() : null));
                productoRepository.save(p);
            }
        }
    }

    /* ══════════════ Perfiles y usuarios (avance 3) ══════════════ */

    private Perfil asegurarPerfil(String nombre, String descripcion) {
        return perfilRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> perfilRepository.save(new Perfil(nombre, descripcion)));
    }

    private void guardarCliente(Perfil perfil, Sucursal almacen, String dni, String telefono, String correo, String nombres,
            String apellidos, String usuario, String estado) {
        Usuario u = new Usuario();
        u.setPerfil(perfil);
        u.setSucursal(almacen);
        u.setDni(dni);
        u.setTelefono(telefono);
        u.setCorreo(correo);
        u.setNombresApellidos(nombres, apellidos);
        u.setUsuario(usuario);
        u.setClave(new BCryptPasswordEncoder().encode(CLAVE_ADMIN));
        u.setEstado(estado);
        usuarioRepository.save(u);
    }

    /**
     * Si la BD ya existia antes de este avance, las columnas id_rol e id_sucursal
     * quedaron como NOT NULL (ddl-auto=update no las relaja). Un cliente que se
     * registra no tiene rol ni sucursal, asi que se vuelven opcionales.
     */
    private void relajarColumnasOpcionales() {
        // Conexion JDBC propia: un ALTER fallido no debe afectar la transaccion JPA.
        for (String sql : new String[] {
                "ALTER TABLE usuarios MODIFY id_rol INT NULL",
                "ALTER TABLE usuarios MODIFY id_sucursal INT NULL",
                // Trabajo final: el pedido ahora pasa por "pagado" y "entregado"
                "ALTER TABLE pedidos DROP CHECK CK_pedidos_estado" }) {
            try (Connection cn = dataSource.getConnection(); Statement st = cn.createStatement()) {
                st.execute(sql);
            } catch (Exception e) {
                log.debug("No se pudo ajustar la columna ({}): {}", sql, e.getMessage());
            }
        }
    }

    /**
     * Usuarios creados antes de este avance: se les asigna un perfil y se separa
     * nombre_completo en nombres / apellidos. El DNI queda vacio hasta que el
     * administrador lo complete desde el CRUD de usuarios.
     */
    private void completarUsuariosAntiguos(Perfil perfilAdmin, Perfil perfilCliente) {
        for (Usuario u : usuarioRepository.findAll()) {
            boolean cambio = false;
            if (u.getPerfil() == null) {
                u.setPerfil(u.getRol() != null ? perfilAdmin : perfilCliente);
                cambio = true;
            }
            if (u.getNombres() == null && u.getNombreCompleto() != null) {
                String[] partes = u.getNombreCompleto().trim().split("\\s+", 2);
                u.setNombresApellidos(partes[0], partes.length > 1 ? partes[1] : "");
                cambio = true;
            }
            if (!UsuarioService.ESTADOS.contains(u.getEstado())) {
                u.setEstado(Usuario.ESTADO_ANULADO);   // p. ej. "inactivo" del avance anterior
                cambio = true;
            }
            if (cambio) {
                usuarioRepository.save(u);
            }
        }
    }

    private Rol guardarRol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setUsuarios(new ArrayList<>());
        return rolRepository.save(rol);
    }

    private Sucursal guardarSucursal(String nombre, String ciudad, String encargado, String telefono, String estado) {
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre(nombre);
        sucursal.setCiudad(ciudad);
        sucursal.setEncargado(encargado);
        sucursal.setTelefono(telefono);
        sucursal.setEstado(estado);
        sucursal.setFechaRegistro(LocalDateTime.now().minusDays(45));
        sucursal.setUsuarios(new ArrayList<>());
        sucursal.setMovimientos(new ArrayList<>());
        sucursal.setSolicitudes(new ArrayList<>());
        return sucursalRepository.save(sucursal);
    }

    private Proveedor guardarProveedor(String nombre, String contacto, String telefono, String email) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre);
        proveedor.setContacto(contacto);
        proveedor.setTelefono(telefono);
        proveedor.setEmail(email);
        proveedor.setMovimientos(new ArrayList<>());
        return proveedorRepository.save(proveedor);
    }

    private Categoria guardarCategoria(String nombre, String descripcion) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        categoria.setProductos(new ArrayList<>());
        return categoriaRepository.save(categoria);
    }

    private Producto guardarProducto(String codigo, String nombre, Categoria categoria, String talla, String color,
            String colorHex, int stock, int stockMinimo, String ubicacion) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setTalla(talla);
        producto.setColor(color);
        producto.setColorHex(colorHex);
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        producto.setUbicacion(ubicacion);
        producto.setPrecio(precioSugerido(categoria.getNombre()));
        return productoRepository.save(producto);
    }

    private void agregarDetalleMovimiento(Movimiento movimiento, Producto producto, int cantidad) {
        DetalleMovimiento detalle = new DetalleMovimiento();
        detalle.setMovimiento(movimiento);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        movimiento.getDetalles().add(detalle);
    }

    private void agregarDetalleSolicitud(Solicitud solicitud, Producto producto, int cantidad) {
        DetalleSolicitud detalle = new DetalleSolicitud();
        detalle.setSolicitud(solicitud);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        solicitud.getDetalles().add(detalle);
    }

}
