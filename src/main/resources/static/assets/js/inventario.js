(function () {
  function estadoDe(p) {
    if (p.stock === 0) return { key: "out", label: "Agotado", cls: "badge-out" };
    if (p.stock <= p.minimo) return { key: "low", label: "Stock bajo", cls: "badge-low" };
    return { key: "ok", label: "En stock", cls: "badge-ok" };
  }

  function poblarCategorias() {
    var select = document.getElementById("filtroCategoria");
    select.querySelectorAll("option:not(:first-child)").forEach(function (opt) { opt.remove(); });
    var categorias = [];
    db.productos.forEach(function (p) {
      if (categorias.indexOf(p.categoria) === -1) categorias.push(p.categoria);
    });
    categorias.forEach(function (c) {
      var opt = document.createElement("option");
      opt.value = c;
      opt.textContent = c;
      select.appendChild(opt);
    });
  }

  function renderInventario() {
    var q = document.getElementById("buscador").value.trim().toLowerCase();
    var cat = document.getElementById("filtroCategoria").value;
    var est = document.getElementById("filtroEstado").value;

    var filtrados = db.productos.filter(function (p) {
      var coincideTexto = !q || p.nombre.toLowerCase().indexOf(q) !== -1 || p.codigo.toLowerCase().indexOf(q) !== -1;
      var coincideCat = !cat || p.categoria === cat;
      var coincideEstado = !est || estadoDe(p).key === est;
      return coincideTexto && coincideCat && coincideEstado;
    });

    document.getElementById("contador").textContent = filtrados.length + " de " + db.productos.length + " productos";

    var tbody = document.getElementById("tablaInventario");
    if (filtrados.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted-soft py-4">No se encontraron productos con esos filtros.</td></tr>';
      return;
    }

    tbody.innerHTML = filtrados.map(function (p) {
      var est = estadoDe(p);
      var pct = Math.min(100, Math.round((p.stock / Math.max(p.minimo * 2, 1)) * 100));
      var barColor = est.key === "out" ? "var(--danger)" : est.key === "low" ? "var(--thread)" : "var(--success)";
      return '<tr>' +
        '<td class="text-muted-soft">' + p.codigo + '</td>' +
        '<td class="fw-semibold">' + p.nombre + '</td>' +
        '<td>' + p.categoria + '</td>' +
        '<td>' + p.talla + '</td>' +
        '<td><span class="swatch-wrap"><span class="swatch" style="background:' + p.hex + '"></span>' + p.color + '</span></td>' +
        '<td class="text-muted-soft">' + p.ubicacion + '</td>' +
        '<td>' +
        '<div class="d-flex align-items-center gap-2">' +
        '<span>' + p.stock + '</span>' +
        '<div class="spool"><span style="width:' + pct + '%; background:' + barColor + ';"></span></div>' +
        '</div>' +
        '</td>' +
        '<td><span class="badge-status ' + est.cls + '">' + est.label + '</span></td>' +
        '<td class="text-end">' +
        '<button class="btn btn-sm btn-outline-denim" data-view-product="' + p.codigo + '"><i class="bi bi-eye"></i></button>' +
        '</td>' +
        '</tr>';
    }).join("");
  }

  function initInventario() {
    document.getElementById("buscador").addEventListener("input", renderInventario);
    document.getElementById("filtroCategoria").addEventListener("change", renderInventario);
    document.getElementById("filtroEstado").addEventListener("change", renderInventario);

    document.getElementById("tablaInventario").addEventListener("click", function (e) {
      if (e.target.closest("[data-view-product]")) {
        alert("Vista de detalle no incluida en esta demo.");
      }
    });

    document.getElementById("formProducto").addEventListener("submit", function (e) {
      e.preventDefault();
      var nuevo = {
        codigo: "TR-" + Math.floor(2000 + Math.random() * 8000),
        nombre: document.getElementById("pNombre").value,
        categoria: document.getElementById("pCategoria").value,
        talla: document.getElementById("pTalla").value,
        color: document.getElementById("pColor").value,
        hex: document.getElementById("pHex").value,
        stock: Number(document.getElementById("pStock").value),
        minimo: Number(document.getElementById("pMinimo").value),
        ubicacion: document.getElementById("pUbicacion").value,
      };
      db.productos.unshift(nuevo);
      tramaSaveDB(db);
      renderInventario();
      poblarCategorias();
      this.reset();
      document.getElementById("pHex").value = "#2F4771";
      bootstrap.Modal.getInstance(document.getElementById("modalProducto")).hide();
    });

    poblarCategorias();
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.inventario = {
    renderInventario: renderInventario,
    poblarCategorias: poblarCategorias,
    initInventario: initInventario,
  };
})();
