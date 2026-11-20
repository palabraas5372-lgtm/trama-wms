// Núcleo: sesión, layout, utilidades y enrutador de la SPA.
// Cada módulo (dashboard, inventario, movimientos, sucursales) aporta su propio
// HTML (assets/pages/) y JS (assets/js/) que se cargan bajo demanda.

const TRAMA_SESSION_KEY = "trama_session";
const TITULOS_PAGINA = {
  dashboard:   { eyebrow: "Panel general",   titulo: "Dashboard" },
  inventario:  { eyebrow: "Almacén central", titulo: "Inventario" },
  movimientos: { eyebrow: "Almacén central", titulo: "Entradas y salidas" },
  sucursales:  { eyebrow: "Red de tiendas",  titulo: "Sucursales" },
};
// Registro global de módulos: cada module JS se autoregistra aquí en window.TramaModules
window.TramaModules = window.TramaModules || {};

// Base de datos simulada, compartida por los módulos
let db = tramaLoadDB();

/** Credenciales de demo (solo front-end, sin backend real) */
const DEMO_USER = { usuario: "admin", clave: "trama2026", nombre: "Diego Contreras", rol: "Jefe de Almacén" };

function tramaLogin(usuario, clave) {
  if (usuario.trim().toLowerCase() === DEMO_USER.usuario && clave === DEMO_USER.clave) {
    sessionStorage.setItem(TRAMA_SESSION_KEY, JSON.stringify({ nombre: DEMO_USER.nombre, rol: DEMO_USER.rol }));
    return true;
  }
  return false;
}

function tramaGetSession() {
  const raw = sessionStorage.getItem(TRAMA_SESSION_KEY);
  return raw ? JSON.parse(raw) : null;
}

function tramaLogout() {
  sessionStorage.removeItem(TRAMA_SESSION_KEY);
  window.location.href = "index.html";
}

/** Pinta el nombre de usuario en el topbar y engancha el botón de logout */
function tramaInitLayout(activePage) {
  const session = tramaGetSession();
  const nameEl = document.querySelector("[data-user-name]");
  const roleEl = document.querySelector("[data-user-role]");
  if (session && nameEl) nameEl.textContent = session.nombre;
  if (session && roleEl) roleEl.textContent = session.rol;

  document.querySelectorAll(".sidebar-nav .nav-link").forEach((link) => {
    if (link.getAttribute("data-page") === activePage) {
      link.classList.add("active");
    }
  });

  const logoutBtn = document.querySelector("[data-logout]");
  if (logoutBtn) logoutBtn.addEventListener("click", (e) => { e.preventDefault(); tramaLogout(); });
}

function tramaFormatNumber(n) {
  return new Intl.NumberFormat("es-PE").format(n);
}

// Carga el HTML de la vista de un módulo dentro de #pageOutlet
function tramaLoadView(page) {
  const out = document.getElementById("pageOutlet");
  return fetch("assets/pages/" + page + ".html")
    .then(function (res) { return res.text(); })
    .then(function (html) {
      out.innerHTML = html;
    });
}

// Carga el script de un módulo una sola vez (no vuelve a descargarlo)
function tramaLoadModule(page) {
  return Promise.resolve().then(function () {
    if (window[page + "Loaded"]) return;
    return new Promise(function (resolve, reject) {
      const script = document.createElement("script");
      script.src = "assets/js/" + page + ".js";
      script.onload = function () {
        window[page + "Loaded"] = true;
        resolve();
      };
      script.onerror = function () { reject(new Error("No se pudo cargar el módulo: " + page)); };
      document.body.appendChild(script);
    });
  });
}

// Vincula los eventos de un módulo. Se ejecuta cada vez que se re-monta su vista,
// por lo que sus listeners se registran sobre nodos recién creados.
function tramaInitModule(page) {
  const boot = { inventario: "initInventario", movimientos: "initMovimientos", sucursales: "initSucursales" }[page];
  const mod = window.TramaModules[page];
  if (boot && mod && typeof mod[boot] === "function") mod[boot]();
}

// Enrutador: renderiza la página activa del módulo actual
function tramaRenderPage(page) {
  const mod = window.TramaModules[page];
  if (!mod) return;
  if (page === "dashboard" && mod.renderDashboard) mod.renderDashboard();
  if (page === "inventario" && mod.renderInventario) mod.renderInventario();
  if (page === "movimientos" && mod.renderHistorialMovimientos) mod.renderHistorialMovimientos();
  if (page === "sucursales") { if (mod.renderSucursales) mod.renderSucursales(); if (mod.renderSolicitudes) mod.renderSolicitudes(); }
}

// Cambia de página en la SPA: carga el HTML del módulo, marca la sidebar/topbar
// y engancha el enrutador a los clics sobre los datos dinámicos.
function tramaShowPage(page) {
  document.querySelectorAll(".sidebar-nav .nav-link").forEach(function (link) {
    link.classList.toggle("active", link.dataset.page === page);
  });

  document.getElementById("topbarEyebrow").textContent = TITULOS_PAGINA[page].eyebrow;
  document.getElementById("topbarTitle").textContent = TITULOS_PAGINA[page].titulo;
  document.getElementById("btnNuevoProducto").classList.toggle("d-none", page !== "inventario");

  tramaLoadModule(page).then(function () {
    return tramaLoadView(page);
  }).then(function () {
    tramaInitModule(page);
    tramaRenderPage(page);
    // Re-engancha los clics del sidebar emitidos dentro de las vistas (p. ej. "Ver todos →")
    document.querySelectorAll(".page-body [data-page]").forEach(function (link) {
      link.addEventListener("click", function (e) {
        e.preventDefault();
        tramaShowPage(link.dataset.page);
      });
    });
  });

  if (window.bootstrap) {
    const offcanvas = bootstrap.Offcanvas.getInstance(document.getElementById("sidebarOffcanvas"));
    if (offcanvas) offcanvas.hide();
  }

  window.location.hash = page;
}

function tramaMostrarApp() {
  document.getElementById("viewLogin").classList.add("d-none");
  document.getElementById("viewApp").classList.remove("d-none");
  tramaInitLayout(null);
  tramaShowPage("dashboard");
}

// Inicialización: login, navegación del sidebar y arranque según sesión activa
(function init() {
  if (tramaGetSession()) {
    tramaMostrarApp();
    return;
  }

  document.getElementById("loginForm").addEventListener("submit", function (e) {
    e.preventDefault();
    const usuario = document.getElementById("usuario").value;
    const clave = document.getElementById("clave").value;
    const errorBox = document.getElementById("loginError");

    if (tramaLogin(usuario, clave)) {
      tramaMostrarApp();
    } else {
      errorBox.textContent = "Usuario o contraseña incorrectos. Intenta nuevamente.";
      errorBox.classList.remove("d-none");
    }
  });

  document.querySelectorAll(".nav-link[data-page]").forEach(function (link) {
    link.addEventListener("click", function (e) {
      e.preventDefault();
      tramaShowPage(link.dataset.page);
    });
  });
})();
