-- TRAMA 

CREATE DATABASE trama_wms;
USE trama_wms;

CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL
);

-- Perfiles de acceso: definen a que ambiente entra el usuario al iniciar sesion
CREATE TABLE perfiles (
    id_perfil INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(120)
);

CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    email VARCHAR(100)
);

CREATE TABLE categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE,
    descripcion VARCHAR(160)
);

CREATE TABLE sucursales (
    id_sucursal INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ciudad VARCHAR(80),
    encargado VARCHAR(100),
    telefono VARCHAR(20),
    estado VARCHAR(20) NOT NULL DEFAULT 'activa',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_perfil INT NULL,
    id_rol INT NULL,
    id_sucursal INT NULL,
    dni CHAR(8) NULL UNIQUE,
    telefono CHAR(9) NULL UNIQUE,
    correo VARCHAR(100) NULL UNIQUE,
    nombres VARCHAR(60),
    apellidos VARCHAR(60),
    usuario VARCHAR(40) NOT NULL UNIQUE,
    clave VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    CONSTRAINT FK_usuarios_perfiles FOREIGN KEY (id_perfil) REFERENCES perfiles(id_perfil),
    CONSTRAINT FK_usuarios_roles FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
    CONSTRAINT FK_usuarios_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT CK_usuarios_estado CHECK (estado IN ('activo', 'pendiente', 'anulado'))
);

CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    id_categoria INT NOT NULL,
    talla VARCHAR(10),
    color VARCHAR(40),
    color_hex VARCHAR(7),
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    ubicacion VARCHAR(20),
    CONSTRAINT FK_productos_categorias FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
    CONSTRAINT CK_productos_stock CHECK (stock >= 0)
);

CREATE TABLE movimientos (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_sucursal INT NULL,
    id_proveedor INT NULL,
    id_usuario INT NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(160),
    CONSTRAINT FK_movimientos_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT FK_movimientos_proveedores FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor),
    CONSTRAINT FK_movimientos_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT CK_movimientos_tipo CHECK (tipo IN ('entrada', 'salida'))
);

CREATE TABLE detalle_movimiento (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_movimiento INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    CONSTRAINT FK_detalle_mov_movimientos FOREIGN KEY (id_movimiento) REFERENCES movimientos(id_movimiento),
    CONSTRAINT FK_detalle_mov_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT CK_detalle_mov_cantidad CHECK (cantidad > 0)
);


CREATE TABLE solicitudes (
    id_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    id_sucursal INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha DATE NOT NULL DEFAULT (CURRENT_DATE),
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    CONSTRAINT FK_solicitudes_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT FK_solicitudes_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT CK_solicitudes_estado CHECK (estado IN ('pendiente', 'en tránsito', 'aprobada'))
);

CREATE TABLE detalle_solicitud (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    CONSTRAINT FK_detalle_sol_solicitudes FOREIGN KEY (id_solicitud) REFERENCES solicitudes(id_solicitud),
    CONSTRAINT FK_detalle_sol_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT CK_detalle_sol_cantidad CHECK (cantidad > 0)
);

-- Pedidos que hacen los clientes desde su portal
CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_sucursal INT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    entregado_en DATETIME NULL,
    festivo_nombre VARCHAR(40) NULL,
    festivo_pct INT NULL,
    telefono VARCHAR(20),
    observacion VARCHAR(200),
    CONSTRAINT FK_pedidos_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT FK_pedidos_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT CK_pedidos_estado CHECK (estado IN ('pendiente', 'pagado', 'entregado', 'atendido', 'cancelado'))
);

CREATE TABLE detalle_pedido (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    CONSTRAINT FK_detalle_ped_pedidos FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    CONSTRAINT FK_detalle_ped_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT CK_detalle_ped_cantidad CHECK (cantidad > 0)
);

-- Ventas: se generan al COBRAR un pedido; guardan el comprobante (boleta o factura)
CREATE TABLE ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL UNIQUE,
    id_cliente INT NOT NULL,
    id_vendedor INT NOT NULL,
    id_sucursal INT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_comprobante VARCHAR(10) NOT NULL,
    serie CHAR(4) NOT NULL,
    numero INT NOT NULL,
    ruc CHAR(11) NULL,
    razon_social VARCHAR(120) NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    monto_recibido DECIMAL(12,2),
    subtotal DECIMAL(12,2) NOT NULL,
    descuento_pct INT NOT NULL DEFAULT 0,
    festivo_nombre VARCHAR(40) NULL,
    festivo_pct INT NULL,
    descuento DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    CONSTRAINT UQ_ventas_comprobante UNIQUE (serie, numero),
    CONSTRAINT FK_ventas_pedidos FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    CONSTRAINT FK_ventas_clientes FOREIGN KEY (id_cliente) REFERENCES usuarios(id_usuario),
    CONSTRAINT FK_ventas_vendedores FOREIGN KEY (id_vendedor) REFERENCES usuarios(id_usuario),
    CONSTRAINT FK_ventas_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT CK_ventas_tipo CHECK (tipo_comprobante IN ('BOLETA', 'FACTURA'))
);

CREATE TABLE detalle_venta (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT FK_detalle_venta_ventas FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
    CONSTRAINT FK_detalle_venta_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT CK_detalle_venta_cantidad CHECK (cantidad > 0)
);

-- Datos

INSERT INTO perfiles (nombre, descripcion) VALUES
('administrador', 'Acceso al panel completo del almacén'),
('cliente', 'Acceso al portal de clientes');

INSERT INTO roles (nombre) VALUES ('Jefe de Almacén'), ('Operador'), ('Encargado de Sucursal');

INSERT INTO sucursales (nombre, ciudad, encargado, telefono, estado) VALUES
('TRAMA Miraflores', 'Lima', 'Rocío Salas', '01-441-2200', 'activa'),
('TRAMA San Isidro', 'Lima', 'Diego Ponte', '01-221-0099', 'activa'),
('TRAMA Trujillo', 'Trujillo', 'Ana Chávez', '044-500-321', 'activa'),
('TRAMA Arequipa', 'Arequipa', 'Iván Torres', '054-330-118', 'en revisión');

-- Clave "trama2026" para todos
INSERT INTO usuarios (id_perfil, id_rol, id_sucursal, dni, telefono, correo, nombres, apellidos, usuario, clave, nombre_completo, estado) VALUES
(1, 1, 1, '40112233', '987000001', 'admin@trama.pe', 'Julián', 'Ramos', 'admin', '$2b$10$ZJ5hxjMcAVUEWtZv5J0DMelC4wNUA7Jqyw7J.ZWgYaQWmm/fFo7rW', 'Julián Ramos', 'activo'),
(2, NULL, 1, '70123456', '987654321', 'maria.quispe@gmail.com', 'María', 'Quispe Rojas', 'cliente', '$2b$10$ZJ5hxjMcAVUEWtZv5J0DMelC4wNUA7Jqyw7J.ZWgYaQWmm/fFo7rW', 'María Quispe Rojas', 'activo'),
(2, NULL, 2, '72345678', '912345678', 'luis.paredes@gmail.com', 'Luis', 'Paredes Soto', 'lparedes', '$2b$10$ZJ5hxjMcAVUEWtZv5J0DMelC4wNUA7Jqyw7J.ZWgYaQWmm/fFo7rW', 'Luis Paredes Soto', 'pendiente');

INSERT INTO proveedores (nombre, contacto, telefono, email) VALUES
('Proveedor Textil Andina', 'Mario Reyes', '01-500-1111', 'ventas@textilandina.pe'),
('Proveedor Confecciones Sur', 'Lucía Prado', '054-600-2222', 'contacto@confeccionessur.pe');

INSERT INTO categorias (nombre) VALUES
('Camisas'), ('Polos'), ('Pantalones'), ('Chompas'), ('Casacas'), ('Faldas'), ('Vestidos');

INSERT INTO productos (codigo, nombre, id_categoria, talla, color, color_hex, stock, stock_minimo, ubicacion) VALUES
('TR-1001', 'Camisa Oxford Clásica', (SELECT id_categoria FROM categorias WHERE nombre='Camisas'), 'M', 'Azul', '#2F4771', 120, 40, 'A-01-03'),
('TR-1002', 'Camisa Oxford Clásica', (SELECT id_categoria FROM categorias WHERE nombre='Camisas'), 'L', 'Blanco', '#F4F2EC', 18, 40, 'A-01-04'),
('TR-1010', 'Polo Piqué Básico', (SELECT id_categoria FROM categorias WHERE nombre='Polos'), 'S', 'Negro', '#1B1F2A', 200, 50, 'A-02-01'),
('TR-1011', 'Polo Piqué Básico', (SELECT id_categoria FROM categorias WHERE nombre='Polos'), 'M', 'Mostaza', '#E8A33D', 6, 50, 'A-02-02'),
('TR-1020', 'Jean Slim Fit', (SELECT id_categoria FROM categorias WHERE nombre='Pantalones'), '32', 'Denim Oscuro', '#223554', 75, 30, 'B-01-01'),
('TR-1021', 'Jean Slim Fit', (SELECT id_categoria FROM categorias WHERE nombre='Pantalones'), '34', 'Denim Claro', '#7C93B8', 0, 30, 'B-01-02'),
('TR-1030', 'Chompa Cuello Redondo', (SELECT id_categoria FROM categorias WHERE nombre='Chompas'), 'M', 'Gris', '#9CA3AF', 54, 25, 'B-02-05'),
('TR-1040', 'Casaca Cortavientos', (SELECT id_categoria FROM categorias WHERE nombre='Casacas'), 'L', 'Verde Olivo', '#5B6B4C', 33, 20, 'C-01-01'),
('TR-1050', 'Falda Plisada', (SELECT id_categoria FROM categorias WHERE nombre='Faldas'), 'S', 'Terracota', '#C1483B', 41, 15, 'C-02-03'),
('TR-1060', 'Vestido Manga Larga', (SELECT id_categoria FROM categorias WHERE nombre='Vestidos'), 'M', 'Vino', '#7A2E38', 22, 20, 'C-03-01');

-- Precios por unidad (venta al por mayor) según la categoría
UPDATE productos p JOIN categorias c ON c.id_categoria = p.id_categoria
SET p.precio = CASE c.nombre
    WHEN 'Camisas' THEN 45 WHEN 'Polos' THEN 25 WHEN 'Pantalones' THEN 60 WHEN 'Chompas' THEN 55
    WHEN 'Casacas' THEN 85 WHEN 'Faldas' THEN 40 WHEN 'Vestidos' THEN 70 ELSE 30 END
WHERE p.id_producto > 0;

-- Ejemplo de una operación con varios productos
INSERT INTO movimientos (id_sucursal, id_proveedor, id_usuario, tipo, fecha, observacion) VALUES
(NULL, 1, 1, 'entrada', '2026-08-25 09:12', 'Recepción de mercadería del proveedor');
SET @mov1 = LAST_INSERT_ID();

INSERT INTO detalle_movimiento (id_movimiento, id_producto, cantidad) VALUES
(@mov1, (SELECT id_producto FROM productos WHERE codigo='TR-1001'), 60),
(@mov1, (SELECT id_producto FROM productos WHERE codigo='TR-1040'), 33);

INSERT INTO movimientos (id_sucursal, id_proveedor, id_usuario, tipo, fecha, observacion) VALUES
(1, NULL, 1, 'salida', '2026-08-25 11:40', 'Despacho a sucursal Miraflores');
SET @mov2 = LAST_INSERT_ID();

INSERT INTO detalle_movimiento (id_movimiento, id_producto, cantidad) VALUES
(@mov2, (SELECT id_producto FROM productos WHERE codigo='TR-1011'), 30);

-- Solicitudes de ejemplo
INSERT INTO solicitudes (codigo, id_sucursal, id_usuario, fecha, estado) VALUES
('SOL-3001', 1, 1, '2026-08-25', 'pendiente');
SET @sol1 = LAST_INSERT_ID();

INSERT INTO detalle_solicitud (id_solicitud, id_producto, cantidad) VALUES
(@sol1, (SELECT id_producto FROM productos WHERE codigo='TR-1011'), 40),
(@sol1, (SELECT id_producto FROM productos WHERE codigo='TR-1021'), 20);

SELECT *
FROM movimientos
WHERE DATE(fecha) = CURDATE()
ORDER BY fecha DESC;

SELECT COUNT(*) AS movimientos_7_dias
FROM movimientos
WHERE fecha >= DATE_SUB(NOW(), INTERVAL 7 DAY);