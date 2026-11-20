// Utilidades compartidas del proyecto TRAMA.
// Funciones globales reutilizables: saneamiento HTML, sistema de notificaciones
// (toasts), formateo de fechas y números, y helpers de DOM.

/**
 * Escapa caracteres HTML para prevenir inyección de código (XSS)
 * cuando se inserta contenido dinámico (usuario o BD) vía innerHTML.
 * @param {*} value - valor a sanear
 * @returns {string} texto seguro para innerHTML
 */
function tramaEscapeHtml(value) {
  return String(value == null ? "" : value).replace(/[&<>"']/g, function (c) {
    return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
  });
}

/**
 * Sistema de notificaciones toast (esquinas, estilo Bootstrap).
 * Crea el contenedor una sola vez y apila avisos auto-descartables.
 * @param {string} mensaje - texto a mostrar
 * @param {string} tipo - "success" | "danger" | "warning" | "info" (default)
 */
function tramaToast(mensaje, tipo) {
  var contenedor = document.getElementById("toastContainer");
  if (!contenedor) {
    contenedor = document.createElement("div");
    contenedor.id = "toastContainer";
    contenedor.className = "toast-container position-fixed bottom-0 end-0 p-3";
    document.body.appendChild(contenedor);
  }

  var iconos = { success: "bi-check-circle-fill", danger: "bi-x-circle-fill", warning: "bi-exclamation-triangle-fill", info: "bi-info-circle-fill" };
  var icono = iconos[tipo] || iconos.info;

  var el = document.createElement("div");
  el.className = "toast align-items-center border-0 show trama-toast toast-" + (tipo || "info");
  el.setAttribute("role", "alert");
  el.innerHTML =
    '<div class="d-flex">' +
    '<div class="toast-body d-flex align-items-center gap-2"><i class="bi ' + icono + '"></i><span>' + tramaEscapeHtml(mensaje) + "</span></div>" +
    '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button>' +
    "</div>";

  contenedor.appendChild(el);
  setTimeout(function () {
    el.classList.remove("show");
    setTimeout(function () { el.remove(); }, 300);
  }, 3500);
}

/**
 * Formatea un número con el locale peruano (es-PE).
 * @param {number} n
 * @returns {string}
 */
function tramaFormatNumber(n) {
  return new Intl.NumberFormat("es-PE").format(n);
}

/**
 * Formatea una fecha ISO (YYYY-MM-DD HH:mm) a una fecha legible corta.
 * @param {string} fechaISO
 * @returns {string}
 */
function tramaFormatFecha(fechaISO) {
  if (!fechaISO) return "—";
  var partes = fechaISO.split(" ");
  var fecha = partes[0] || "";
  var hora = partes[1] || "";
  var d = fecha.split("-");
  if (d.length !== 3) return fechaISO;
  var meses = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
  var corta = d[2] + " " + meses[Number(d[1]) - 1] + " " + d[0];
  return hora ? corta + " · " + hora : corta;
}

/**
 * Devuelve un badge de estado reutilizable (HTML seguro).
 * @param {string} texto - texto del badge
 * @param {string} clase - clase CSS del badge (badge-ok, badge-low, ...)
 * @returns {string} HTML
 */
function tramaBadge(texto, clase) {
  return '<span class="badge-status ' + clase + '">' + tramaEscapeHtml(texto) + "</span>";
}

// Exponer utilidades globalmente (ya son funciones globales por declaración)