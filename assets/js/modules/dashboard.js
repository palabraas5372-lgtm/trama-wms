// Módulo Dashboard: KPIs, alertas de reposición, movimientos recientes y
// gráficos de análisis (Chart.js). Se autoregistra en window.TramaModules.

(function () {
  // Referencias a instancias activas de Chart.js para destruirlas al re-render
  var chartCategorias = null;
  var chartEstados = null;

  function renderKPIs() {
    document.getElementById("kpiProductos").textContent = tramaFormatNumber(db.productos.length);
    document.getElementById("kpiBajo").textContent = db.productos.filter(function (p) { return p.stock <= p.minimo; }).length;
    document.getElementById("kpiSucursales").textContent =
      db.sucursales.filter(function (s) { return s.estado === "activa"; }).length + " / " + db.sucursales.length;
    document.getElementById("kpiMovimientos").textContent = tramaFormatNumber(db.movimientos.length);
  }

  function renderAlertas() {
    var alertList = document.getElementById("alertList");
    var bajos = db.productos.filter(function (p) { return p.stock <= p.minimo; }).sort(function (a, b) { return a.stock - b.stock; });

    if (bajos.length === 0) {
      alertList.innerHTML = '<div class="p-4 text-center"><i class="bi bi-check2-circle text-success" style="font-size:1.6rem;"></i><div class="text-muted-soft small mt-1">No hay productos por debajo del mínimo. Buen trabajo.</div></div>';
      return;
    }

    alertList.innerHTML = bajos.map(function (p) {
      var isOut = p.stock === 0;
      return '<div class="d-flex justify-content-between align-items-center px-3 py-2" style="border-bottom:1px solid var(--line);">' +
        '<div>' +
        '<div class="fw-semibold small">' + tramaEscapeHtml(p.nombre) + '</div>' +
        '<div class="text-muted-soft small">' + tramaEscapeHtml(p.codigo) + " · Talla " + tramaEscapeHtml(p.talla) + " · " + tramaEscapeHtml(p.color) + "</div>" +
        "</div>" +
        tramaBadge(isOut ? "Agotado" : p.stock + " uds.", isOut ? "badge-out" : "badge-low") +
        "</div>";
    }).join("");
  }

  function renderMovimientos() {
    document.getElementById("movBody").innerHTML = db.movimientos.slice(0, 6).map(function (m) {
      return "<tr>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.fecha) + "</td>" +
        "<td>" + tramaBadge(m.tipo === "entrada" ? "Entrada" : "Salida", m.tipo === "entrada" ? "badge-ok" : "badge-transit") + "</td>" +
        "<td>" + tramaEscapeHtml(m.producto) + "</td>" +
        "<td>" + tramaFormatNumber(m.cantidad) + "</td>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.origen) + "</td>" +
        "</tr>";
    }).join("");
  }

  function renderGraficos() {
    // Destruye instancias previas para evitar duplicados en cada re-render
    if (chartCategorias) chartCategorias.destroy();
    if (chartEstados) chartEstados.destroy();

    var porCategoria = {};
    db.productos.forEach(function (p) {
      porCategoria[p.categoria] = (porCategoria[p.categoria] || 0) + p.stock;
    });

    var coloresBase = ["#2F4771", "#E8A33D", "#3F8F6F", "#C1483B", "#7A2E38", "#5B6B4C", "#9CA3AF", "#7C93B8"];

    chartCategorias = new Chart(document.getElementById("chartCategorias"), {
      type: "bar",
      data: {
        labels: Object.keys(porCategoria),
        datasets: [{
          label: "Unidades en stock",
          data: Object.values(porCategoria),
          backgroundColor: "#2F4771",
          borderRadius: 6,
          maxBarThickness: 42,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, grid: { color: "rgba(27,31,42,.06)" }, ticks: { font: { family: "Inter" } } },
          x: { grid: { display: false }, ticks: { font: { family: "Inter" } } },
        },
      },
    });

    var ok = db.productos.filter(function (p) { return p.stock > p.minimo; }).length;
    var low = db.productos.filter(function (p) { return p.stock > 0 && p.stock <= p.minimo; }).length;
    var out = db.productos.filter(function (p) { return p.stock === 0; }).length;

    chartEstados = new Chart(document.getElementById("chartEstados"), {
      type: "doughnut",
      data: {
        labels: ["En stock", "Stock bajo", "Agotado"],
        datasets: [{
          data: [ok, low, out],
          backgroundColor: ["#3F8F6F", "#E8A33D", "#C1483B"],
          borderWidth: 0,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "62%",
        plugins: {
          legend: { position: "bottom", labels: { font: { family: "Inter", size: 12 }, usePointStyle: true, padding: 16 } },
        },
      },
    });
  }

  function renderDashboard() {
    renderKPIs();
    renderAlertas();
    renderMovimientos();
    if (window.Chart) renderGraficos();
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.dashboard = { renderDashboard: renderDashboard };
})();