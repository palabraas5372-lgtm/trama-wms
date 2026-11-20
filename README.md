# TRAMA · Sistema de Gestión de Almacenes

Aplicación web para **TRAMA S.A.**, empresa peruana de confección y distribución textil. Centraliza el inventario del almacén, las entradas y salidas de mercadería, las solicitudes de las sucursales y la venta al por mayor a clientes.

Curso: Marcos de Desarrollo Web — Universidad Tecnológica del Perú (2026)
Estudiantes: Diego Armando Contreras Velasquez · Reidelinda Ramírez Yahuarcani

## Tecnologías

- Java 25 · Spring Boot 4 (Web MVC, Thymeleaf, Data JPA / Hibernate)
- MySQL 8
- Bootstrap 5, Chart.js
- BCrypt (contraseñas cifradas) · OpenPDF (boletas y facturas en PDF)

## Funcionalidades

**Administrador**
- Dashboard con indicadores, alertas de stock y gráficos.
- CRUD de productos, categorías, proveedores, sucursales, roles y usuarios.
- Entradas y salidas de mercadería con actualización automática del stock.
- Solicitudes de abastecimiento de sucursales.
- Pedidos de clientes: **cobrar** (efectivo, transferencia, Yape/Plin o tarjeta), emitir **boleta o factura en PDF** y marcar como **entregado**.
- Ventas emitidas y **reporte de ventas por mes y por año**.

**Cliente**
- Registro con validación de DNI, usuario, teléfono y correo únicos (la cuenta la activa un administrador).
- Catálogo con precios, pedidos al por mayor (mínimo 10 unidades por producto).
- **Descuentos por volumen**: 5 % desde 50 unidades, 10 % desde 100 y 15 % desde 200.
- **Descuentos por fechas especiales del Perú**: Navidad, Año Nuevo, Fiestas Patrias y Día de la Madre.
- Seguimiento de pedidos y descarga de su comprobante en PDF.

## Cómo ejecutarlo

1. Tener MySQL en ejecución y ajustar usuario y contraseña en `src/main/resources/application.properties`.
2. Ejecutar:
   ```bash
   ./mvnw spring-boot:run        # Windows: .\mvnw spring-boot:run
   ```
3. Abrir http://localhost:8080

La base de datos `trama_wms` y sus tablas se crean automáticamente, junto con datos de prueba. El script completo está en `src/main/resources/db/trama_wms.sql`.

**Cuentas de prueba** (contraseña `trama2026`): `admin` (administrador) y `cliente` (cliente).

Para probar las campañas por fechas especiales, se puede simular un día en `application.properties`:
```properties
trama.descuentos.fecha-simulada=2026-12-20
```
