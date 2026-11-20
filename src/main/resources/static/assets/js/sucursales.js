(function () {
  function renderSucursales() {
    document.getElementById("gridSucursales").innerHTML = db.sucursales.map(function (s) {
      return '<div class="col-md-6 col-xl-3">' +
        '<div class="card-surface p-3 h-100">' +
        '<div class="d-flex justify-content-between align-items-start mb-2">' +
        '<h3 class="h6 mb-0">' + s.nombre + '</h3>' +
        '<span class="badge-status ' + (s.estado === "activa" ? "badge-ok" : "badge-pending") + '">' + (s.estado === "activa" ? "Activa" : "En revisión") + '</span>' +
        '</div>' +
        '<div class="text-muted-soft small mb-1"><i class="bi bi-geo-alt me-1"></i>' + s.ciudad + '</div>' +
        '<div class="text-muted-soft small mb-1"><i class="bi bi-person me-1"></i>' + s.encargado + '</div>' +
        '<div class="text-muted-soft small mb-3"><i class="bi bi-telephone me-1"></i>' + s.telefono + '</div>' +
        '<hr class="stitch-divider mb-2">' +
        '<div class="d-flex justify-content-between align-items-center">' +
        '<span class="small">Solicitudes activas</span>' +
        '<span class="fw-display fw-bold">' + s.solicitudesActivas + '</span>' +
        '</div>' +
        '</div>' +
        '</div>';
    }).join("");
  }

  function badgeSolicitud(estado) {
    if (estado === "pendiente") return "badge-pending";
    if (estado === "en tránsito") return "badge-transit";
    if (estado === "aprobada") return "badge-approved";
    return "badge-pending";
  }

  function renderSolicitudes() {
    document.getElementById("tablaSolicitudes").innerHTML = db.solicitudes.map(function (s) {
      return '<tr>' +
        '<td class="text-muted-soft">' + s.id + '</td>' +
        '<td class="fw-semibold">' + s.sucursal + '</td>' +
        '<td>' + s.producto + '</td>' +
        '<td>' + s.cantidad + '</td>' +
        '<td class="text-muted-soft">' + s.fecha + '</td>' +
        '<td><span class="badge-status ' + badgeSolicitud(s.estado) + '">' + s.estado + '</span></td>' +
        '<td class="text-end">' +
        (s.estado === "pendiente"
          ? '<button class="btn btn-sm btn-denim" data-aprobar-id="' + s.id + '"><i class="bi bi-check-lg me-1"></i>Aprobar</button>'
          : '<span class="text-muted-soft small">—</span>') +
        '</td>' +
        '</tr>';
    }).join("");
  }

  function initSucursales() {
    document.getElementById("tablaSolicitudes").addEventListener("click", function (e) {
      var btn = e.target.closest("[data-aprobar-id]");
      if (!btn) return;
      var sol = db.solicitudes.find(function (s) { return s.id === btn.dataset.aprobarId; });
      if (sol) sol.estado = "aprobada";
      tramaSaveDB(db);
      renderSolicitudes();
    });
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.sucursales = {
    renderSucursales: renderSucursales,
    renderSolicitudes: renderSolicitudes,
    initSucursales: initSucursales,
  };
})();
