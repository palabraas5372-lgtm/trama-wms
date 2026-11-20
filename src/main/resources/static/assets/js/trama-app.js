// Núcleo del index.html: enrutado por hash entre los módulos embebidos, gestión de
// los modales de alta/edición y el estado del sidebar y del topbar.
//
// No hace peticiones HTTP: todos los módulos ya vienen renderizados en #pageOutlet,
// solo se muestra u oculta el activo. Las altas, editions y eliminaciones sí viajan
// al servidor y vuelven con un redirect a /index#<modulo>.

(function () {

  // ── Módulos y títulos del topbar ──────────────────────────────────
  var MODULOS = {
    dashboard: { eyebrow: 'Panel general', titulo: 'Dashboard' },
    productos: { eyebrow: 'Almacén central', titulo: 'Inventario' },
    movimientos: { eyebrow: 'Almacén central', titulo: 'Entradas y salidas' },
    sucursales: { eyebrow: 'Red de tiendas', titulo: 'Sucursales' },
    solicitudes: { eyebrow: 'Red de tiendas', titulo: 'Solicitudes' },
    pedidos: { eyebrow: 'Clientes', titulo: 'Pedidos de clientes' },
    ventas: { eyebrow: 'Clientes', titulo: 'Ventas y comprobantes' },
    reportes: { eyebrow: 'Clientes', titulo: 'Reporte de ventas' },
    categorias: { eyebrow: 'Catálogo', titulo: 'Categorías' },
    proveedores: { eyebrow: 'Catálogo', titulo: 'Proveedores' },
    usuarios: { eyebrow: 'Seguridad', titulo: 'Usuarios' },
    roles: { eyebrow: 'Seguridad', titulo: 'Roles' }
  };
  var MODULO_INICIAL = 'dashboard';

  // ── Títulos de los modales, por id de modal ────────────────────────
  var TITULOS_MODAL = {
    modalProducto: { nuevo: 'Registrar nuevo producto', editar: 'Editar producto' },
    modalMovimiento: { nuevo: 'Registrar movimiento', editar: 'Editar movimiento' },
    modalSucursal: { nuevo: 'Registrar sucursal', editar: 'Editar sucursal' },
    modalSolicitud: { nuevo: 'Registrar solicitud', editar: 'Editar solicitud' },
    modalCategoria: { nuevo: 'Registrar categoría', editar: 'Editar categoría' },
    modalProveedor: { nuevo: 'Registrar proveedor', editar: 'Editar proveedor' },
    modalUsuario: { nuevo: 'Registrar usuario', editar: 'Editar usuario' },
    modalRol: { nuevo: 'Registrar rol', editar: 'Editar rol' }
  };

  // ══════════════ Enrutado ══════════════

  // ── Opción de retorno: pila de módulos visitados ──
  var historial = [];
  var moduloActual = null;

  function volver() {
    var anterior = historial.pop() || MODULO_INICIAL;
    mostrarModulo(anterior, true);
  }

  // Gráfico del reporte de ventas por mes
  var graficoReporte = null;
  function renderReporte() {
    var datos = window.TRAMA_CHARTS || {};
    var lienzo = document.getElementById('chartVentasMes');
    if (!lienzo || !window.Chart) return;
    if (graficoReporte) graficoReporte.destroy();
    graficoReporte = new Chart(lienzo, {
      type: 'bar',
      data: {
        labels: datos.reporteMesesLabels || [],
        datasets: [{ label: 'Total vendido ' + (datos.reporteAnio || '') + ' (S/)',
          data: datos.reporteMesesTotales || [], backgroundColor: '#2F4771', borderRadius: 4 }]
      },
      options: { responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { callback: function (v) { return 'S/ ' + v; } } } } }
    });
  }

  function mostrarModulo(nombre, esRetorno) {
    var modulo = MODULOS[nombre] ? nombre : MODULO_INICIAL;
    if (!esRetorno && moduloActual && moduloActual !== modulo) {
      historial.push(moduloActual);
    }
    moduloActual = modulo;
    var btnVolver = document.getElementById('btnVolver');
    if (btnVolver) btnVolver.classList.toggle('d-none', modulo === MODULO_INICIAL && historial.length === 0);

    // Muestra solo la sección del módulo activo
    document.querySelectorAll('[data-modulo]').forEach(function (seccion) {
      seccion.classList.toggle('d-none', seccion.getAttribute('data-modulo') !== modulo);
    });

    // Sidebar
    document.querySelectorAll('.sidebar-nav .nav-link').forEach(function (link) {
      link.classList.toggle('active', link.getAttribute('data-page') === modulo);
    });

    // Topbar
    document.getElementById('topbarEyebrow').textContent = MODULOS[modulo].eyebrow;
    document.getElementById('topbarTitle').textContent = MODULOS[modulo].titulo;

    // Los gráficos solo se pintan cuando el dashboard está a la vista
    if (modulo === 'dashboard' && window.tramaRenderCharts) {
      window.tramaRenderCharts();
    }
    if (modulo === 'reportes') {
      renderReporte();
    }

    // En móvil el sidebar es un offcanvas: se cierra al cambiar de módulo
    var lateral = document.getElementById('sidebarOffcanvas');
    if (lateral && window.bootstrap) {
      var instancia = bootstrap.Offcanvas.getInstance(lateral);
      if (instancia) instancia.hide();
    }

    if (window.location.hash !== '#' + modulo) {
      window.history.replaceState(null, '', '#' + modulo);
    }
  }

  function moduloDelHash() {
    return (window.location.hash || '').replace(/^#/, '') || MODULO_INICIAL;
  }

  // ══════════════ Modales ══════════════

  function limpiarCampos(modal) {
    // Restablece cada campo a su valor por defecto del HTML, de modo que los
    // defaults (stock 0, color #2F4771, etc.) se respetan al crear uno nuevo.
    modal.querySelectorAll('input, textarea').forEach(function (campo) {
      if (campo.type === 'checkbox' || campo.type === 'radio') { campo.checked = false; return; }
      campo.value = campo.defaultValue;
    });
    modal.querySelectorAll('select').forEach(function (campo) { campo.selectedIndex = 0; });
  }

  function rellenarCampos(modal, boton) {
    // Los data-* del botón se comparan en minúsculas contra el name de cada campo,
    // así que data-colorhex rellena name="colorHex".
    var datos = boton.dataset;
    modal.querySelectorAll('input, textarea, select').forEach(function (campo) {
      var clave = (campo.getAttribute('name') || '').toLowerCase();
      if (clave && Object.prototype.hasOwnProperty.call(datos, clave)) {
        campo.value = datos[clave];
      }
    });
  }

  function actualizarEstadoProducto(modal) {
    var stock = parseInt(modal.querySelector('[name="stock"]').value, 10) || 0;
    var minimo = parseInt(modal.querySelector('[name="stockMinimo"]').value, 10) || 0;
    var insignia = modal.querySelector('#estadoProducto');
    if (!insignia) return;

    insignia.className = 'badge-status ';
    if (stock === 0) {
      insignia.classList.add('badge-out');
      insignia.textContent = 'Agotado';
    } else if (stock <= minimo) {
      insignia.classList.add('badge-low');
      insignia.textContent = 'Stock bajo';
    } else {
      insignia.classList.add('badge-ok');
      insignia.textContent = 'En stock';
    }
  }

  function abrirModal(idModal, boton) {
    var modal = document.getElementById(idModal);
    if (!modal || !window.bootstrap) return;

    var modo = boton.getAttribute('data-modo') === 'editar' ? 'editar' : 'nuevo';

    if (modo === 'editar') {
      // En usuarios se limpia antes, para que la clave de una edición anterior no se reenvíe
      if (idModal === 'modalUsuario') limpiarCampos(modal);
      rellenarCampos(modal, boton);
    } else {
      limpiarCampos(modal);
    }

    var titulos = TITULOS_MODAL[idModal];
    var encabezado = modal.querySelector('.modal-title');
    if (titulos && encabezado) {
      encabezado.textContent = titulos[modo];
    }

    if (idModal === 'modalProducto') {
      actualizarEstadoProducto(modal);
    }

    // Los modulos con estado propio (por ejemplo, el detalle del movimiento)
    // reciben aqui un gancho para armarse antes de que se vea el modal.
    if (window.tramaModalAbierto) {
      window.tramaModalAbierto(idModal, modo, boton);
    }

    bootstrap.Modal.getOrCreateInstance(modal).show();
  }

  // ══════════════ Avisos del servidor ══════════════

  function mostrarAvisos() {
    if (!window.bootstrap) return;
    document.querySelectorAll('.trama-toast').forEach(function (aviso) {
      bootstrap.Toast.getOrCreateInstance(aviso, { delay: 6000 }).show();
    });
  }

  // ══════════════ Arranque ══════════════

  document.addEventListener('click', function (evento) {

    // Enlaces del sidebar y enlaces internos entre módulos (#movimientos, etc.)
    var enlace = evento.target.closest('a[data-page]');
    if (enlace) {
      evento.preventDefault();
      mostrarModulo(enlace.getAttribute('data-page'));
      return;
    }

    // Confirmación antes de seguir un enlace de eliminación.
    // Thymeleaf no permite construir el texto dentro de onclick, así que cada
    // botón expone su mensaje en data-confirm.
    var eliminable = evento.target.closest('a[data-confirm]');
    if (eliminable) {
      if (!window.confirm(eliminable.getAttribute('data-confirm'))) {
        evento.preventDefault();
      }
      return;
    }

    // Botones que abren un modal de alta o edición
    var boton = evento.target.closest('[data-modal]');
    if (boton) {
      evento.preventDefault();
      abrirModal(boton.getAttribute('data-modal'), boton);
    }
  });

  // El estado del producto se recalcula mientras se escribe
  document.addEventListener('input', function (evento) {
    var modal = evento.target.closest('#modalProducto');
    if (modal && (evento.target.name === 'stock' || evento.target.name === 'stockMinimo')) {
      actualizarEstadoProducto(modal);
    }
  });

  window.addEventListener('hashchange', function () {
    mostrarModulo(moduloDelHash());
  });

  document.addEventListener('DOMContentLoaded', function () {
    var btnVolver = document.getElementById('btnVolver');
    if (btnVolver) btnVolver.addEventListener('click', volver);
    mostrarAvisos();
    mostrarModulo(moduloDelHash());
  });

})();
