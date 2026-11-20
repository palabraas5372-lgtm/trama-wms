(function () {
  function renderDashboard() {
    document.getElementById("kpiProductos").textContent = tramaFormatNumber(db.productos.length);
    document.getElementById("kpiBajo").textContent = db.productos.filter(function (p) { return p.stock <= p.minimo; }).length;
    document.getElementById("kpiSucursales").textContent =
      db.sucursales.filter(function (s) { return s.estado === "activa"; }).length + " / " + db.sucursales.length;
    document.getElementById("kpiMovimientos").textContent = db.movimientos.length;

    var alertList = document.getElementById("alertList");
    var bajos = db.productos.filter(function (p) { return p.stock <= p.minimo; }).sort(function (a, b) { return a.stock - b.stock; });
    if (bajos.length === 0) {
      alertList.innerHTML = '<div class="p-4 text-muted-soft small">No hay productos por debajo del mínimo. Buen trabajo.</div>';
    } else {
      alertList.innerHTML = bajos.map(function (p) {
        var isOut = p.stock === 0;
        return '<div class="d-flex justify-content-between align-items-center px-3 py-2" style="border-bottom:1px solid var(--line);">' +
          '<div>' +
          '<div class="fw-semibold small">' + p.nombre + '</div>' +
          '<div class="text-muted-soft small">' + p.codigo + ' · Talla ' + p.talla + ' · ' + p.color + '</div>' +
          '</div>' +
          '<span class="badge-status ' + (isOut ? "badge-out" : "badge-low") + '">' + (isOut ? "Agotado" : p.stock + " uds.") + '</span>' +
          '</div>';
      }).join("");
    }

    document.getElementById("movBody").innerHTML = db.movimientos.slice(0, 6).map(function (m) {
      return '<tr>' +
        '<td class="text-muted-soft">' + m.fecha + '</td>' +
        '<td><span class="badge-status ' + (m.tipo === "entrada" ? "badge-ok" : "badge-transit") + '">' + (m.tipo === "entrada" ? "Entrada" : "Salida") + '</span></td>' +
        '<td>' + m.producto + '</td>' +
        '<td>' + m.cantidad + '</td>' +
        '<td class="text-muted-soft">' + m.origen + '</td>' +
        '</tr>';
    }).join("");
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.dashboard = { renderDashboard: renderDashboard };
})();
