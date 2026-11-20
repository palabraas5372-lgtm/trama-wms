// Datos simulados: no hay backend, todo vive en localStorage

const TRAMA_DB_KEY = "trama_db_v1";

const SEED_DATA = {
  productos: [
    { codigo: "TR-1001", nombre: "Camisa Oxford Clásica", categoria: "Camisas", talla: "M", color: "Azul", hex: "#2F4771", stock: 120, minimo: 40, ubicacion: "A-01-03" },
    { codigo: "TR-1002", nombre: "Camisa Oxford Clásica", categoria: "Camisas", talla: "L", color: "Blanco", hex: "#F4F2EC", stock: 18, minimo: 40, ubicacion: "A-01-04" },
    { codigo: "TR-1010", nombre: "Polo Piqué Básico", categoria: "Polos", talla: "S", color: "Negro", hex: "#1B1F2A", stock: 200, minimo: 50, ubicacion: "A-02-01" },
    { codigo: "TR-1011", nombre: "Polo Piqué Básico", categoria: "Polos", talla: "M", color: "Mostaza", hex: "#E8A33D", stock: 6, minimo: 50, ubicacion: "A-02-02" },
    { codigo: "TR-1020", nombre: "Jean Slim Fit", categoria: "Pantalones", talla: "32", color: "Denim Oscuro", hex: "#223554", stock: 75, minimo: 30, ubicacion: "B-01-01" },
    { codigo: "TR-1021", nombre: "Jean Slim Fit", categoria: "Pantalones", talla: "34", color: "Denim Claro", hex: "#7C93B8", stock: 0, minimo: 30, ubicacion: "B-01-02" },
    { codigo: "TR-1030", nombre: "Chompa Cuello Redondo", categoria: "Chompas", talla: "M", color: "Gris", hex: "#9CA3AF", stock: 54, minimo: 25, ubicacion: "B-02-05" },
    { codigo: "TR-1040", nombre: "Casaca Cortavientos", categoria: "Casacas", talla: "L", color: "Verde Olivo", hex: "#5B6B4C", stock: 33, minimo: 20, ubicacion: "C-01-01" },
    { codigo: "TR-1050", nombre: "Falda Plisada", categoria: "Faldas", talla: "S", color: "Terracota", hex: "#C1483B", stock: 41, minimo: 15, ubicacion: "C-02-03" },
    { codigo: "TR-1060", nombre: "Vestido Manga Larga", categoria: "Vestidos", talla: "M", color: "Vino", hex: "#7A2E38", stock: 22, minimo: 20, ubicacion: "C-03-01" },
  ],

  movimientos: [
    { fecha: "2026-08-25 09:12", tipo: "entrada", codigo: "TR-1001", producto: "Camisa Oxford Clásica", cantidad: 60, origen: "Proveedor Textil Andina", responsable: "J. Ramos" },
    { fecha: "2026-08-25 11:40", tipo: "salida", codigo: "TR-1011", producto: "Polo Piqué Básico", cantidad: 30, origen: "Sucursal Miraflores", responsable: "L. Cárdenas" },
    { fecha: "2026-08-24 16:05", tipo: "salida", codigo: "TR-1021", producto: "Jean Slim Fit", cantidad: 25, origen: "Sucursal San Isidro", responsable: "J. Ramos" },
    { fecha: "2026-08-24 10:20", tipo: "entrada", codigo: "TR-1040", producto: "Casaca Cortavientos", cantidad: 33, origen: "Proveedor Confecciones Sur", responsable: "M. Vidal" },
    { fecha: "2026-08-23 14:55", tipo: "salida", codigo: "TR-1002", producto: "Camisa Oxford Clásica", cantidad: 12, origen: "Sucursal Trujillo", responsable: "L. Cárdenas" },
  ],

  sucursales: [
    { nombre: "TRAMA Miraflores", ciudad: "Lima", encargado: "Rocío Salas", telefono: "01-441-2200", solicitudesActivas: 2, estado: "activa" },
    { nombre: "TRAMA San Isidro", ciudad: "Lima", encargado: "Diego Ponte", telefono: "01-221-0099", solicitudesActivas: 1, estado: "activa" },
    { nombre: "TRAMA Trujillo", ciudad: "Trujillo", encargado: "Ana Chávez", telefono: "044-500-321", solicitudesActivas: 0, estado: "activa" },
    { nombre: "TRAMA Arequipa", ciudad: "Arequipa", encargado: "Iván Torres", telefono: "054-330-118", solicitudesActivas: 1, estado: "en revisión" },
  ],

  solicitudes: [
    { id: "SOL-3001", sucursal: "TRAMA Miraflores", producto: "Polo Piqué Básico (Mostaza, M)", cantidad: 40, fecha: "2026-08-25", estado: "pendiente" },
    { id: "SOL-3002", sucursal: "TRAMA Miraflores", producto: "Jean Slim Fit (Claro, 34)", cantidad: 20, fecha: "2026-08-25", estado: "pendiente" },
    { id: "SOL-3003", sucursal: "TRAMA San Isidro", producto: "Camisa Oxford (Blanco, L)", cantidad: 25, fecha: "2026-08-24", estado: "en tránsito" },
    { id: "SOL-3004", sucursal: "TRAMA Arequipa", producto: "Casaca Cortavientos (Olivo, L)", cantidad: 15, fecha: "2026-08-23", estado: "aprobada" },
  ],
};

function tramaLoadDB() {
  const raw = localStorage.getItem(TRAMA_DB_KEY);
  if (!raw) {
    localStorage.setItem(TRAMA_DB_KEY, JSON.stringify(SEED_DATA));
    return JSON.parse(JSON.stringify(SEED_DATA));
  }
  try {
    return JSON.parse(raw);
  } catch (e) {
    localStorage.setItem(TRAMA_DB_KEY, JSON.stringify(SEED_DATA));
    return JSON.parse(JSON.stringify(SEED_DATA));
  }
}

function tramaSaveDB(db) {
  localStorage.setItem(TRAMA_DB_KEY, JSON.stringify(db));
}

function tramaResetDB() {
  localStorage.setItem(TRAMA_DB_KEY, JSON.stringify(SEED_DATA));
}
