-- ══════════════════════════════════════════════════════════════════
-- TRAMA WMS · Avance 3 — Login por perfil y registro de usuarios
-- Ejecutar SOLO si ya tenías la BD creada del avance anterior.
-- (La aplicación también aplica estos cambios sola al arrancar gracias a
--  ddl-auto=update + DataInitializer; este script es para hacerlo a mano.)
-- ══════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS perfiles (
    id_perfil INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(120)
);

INSERT IGNORE INTO perfiles (nombre, descripcion) VALUES
('administrador', 'Acceso al panel completo del almacén'),
('cliente', 'Acceso al portal de clientes');

ALTER TABLE usuarios
    ADD COLUMN id_perfil INT NULL AFTER id_usuario,
    ADD COLUMN dni CHAR(8) NULL UNIQUE AFTER id_sucursal,
    ADD COLUMN nombres VARCHAR(60) NULL AFTER dni,
    ADD COLUMN apellidos VARCHAR(60) NULL AFTER nombres,
    MODIFY id_rol INT NULL,
    MODIFY id_sucursal INT NULL,
    MODIFY estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    ADD CONSTRAINT FK_usuarios_perfiles FOREIGN KEY (id_perfil) REFERENCES perfiles(id_perfil);

-- Los usuarios que ya existían (personal del almacén) pasan a ser administradores
UPDATE usuarios
SET id_perfil = (SELECT id_perfil FROM perfiles WHERE nombre = 'administrador')
WHERE id_perfil IS NULL;

-- Separa nombre_completo en nombres / apellidos (primer palabra = nombres)
UPDATE usuarios
SET nombres = SUBSTRING_INDEX(nombre_completo, ' ', 1),
    apellidos = TRIM(SUBSTRING(nombre_completo, LENGTH(SUBSTRING_INDEX(nombre_completo, ' ', 1)) + 1))
WHERE nombres IS NULL;

-- Estados anteriores ("inactivo") se normalizan a "anulado"
UPDATE usuarios SET estado = 'anulado' WHERE estado NOT IN ('activo', 'pendiente', 'anulado');

-- ── Teléfono y correo (únicos, como el DNI) ──
ALTER TABLE usuarios
    ADD COLUMN telefono CHAR(9) NULL UNIQUE AFTER dni,
    ADD COLUMN correo VARCHAR(100) NULL UNIQUE AFTER telefono;

-- ── Almacén que atiende cada pedido ──
ALTER TABLE pedidos
    ADD COLUMN id_sucursal INT NULL AFTER id_usuario,
    ADD CONSTRAINT FK_pedidos_sucursales FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);


-- ══════════════ Trabajo final: precios, cobro, comprobantes y entrega ══════════════
ALTER TABLE productos ADD COLUMN precio DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER stock_minimo;
ALTER TABLE pedidos ADD COLUMN entregado_en DATETIME NULL AFTER estado;
-- (las tablas ventas y detalle_venta están en trama_wms.sql; Hibernate también las crea solo)
ALTER TABLE pedidos ADD COLUMN festivo_nombre VARCHAR(40) NULL, ADD COLUMN festivo_pct INT NULL;
