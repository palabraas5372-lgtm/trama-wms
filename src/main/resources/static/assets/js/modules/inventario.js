// Módulo Inventario: tabla con búsqueda y filtros, registro de productos y
// modal de detalle por producto. Se autoregistra en window.TramaModules.

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
      tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5"><i class="bi bi-inbox text-muted-soft" style="font-size:1.8rem;"></i><div class="text-muted-soft mt-2">No se encontraron productos con esos filtros.</div></td></tr>';
      return;
    }

    tbody.innerHTML = filtrados.map(function (p) {
      var est = estadoDe(p);
      var pct = Math.min(100, Math.round((p.stock / Math.max(p.minimo * 2, 1)) * 100));
      var barColor = est.key === "out" ? "var(--danger)" : est.key === "low" ? "var(--thread)" : "var(--success)";
      return "<tr>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(p.codigo) + "</td>" +
        '<td class="fw-semibold">' + tramaEscapeHtml(p.nombre) + "</td>" +
        "<td>" + tramaEscapeHtml(p.categoria) + "</td>" +
        "<td>" + tramaEscapeHtml(p.talla) + "</td>" +
        '<td><span class="swatch-wrap"><span class="swatch" style="background:' + tramaEscapeHtml(p.hex) + '"></span>' + tramaEscapeHtml(p.color) + "</span></td>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(p.ubicacion) + "</td>" +
        "<td>" +
        '<div class="d-flex align-items-center gap-2">' +
        "<span>" + tramaFormatNumber(p.stock) + "</span>" +
        '<div class="spool"><span style="width:' + pct + "%; background:" + barColor + ';"></span></div>' +
        "</div>" +
        "</td>" +
        "<td>" + tramaBadge(est.label, est.cls) + "</td>" +
        '<td class="text-end">' +
        '<button class="btn btn-sm btn-outline-denim" data-view-product="' + tramaEscapeHtml(p.codigo) + '" title="Ver detalle"><i class="bi bi-eye"></i></button>' +
        "</td>" +
        "</tr>";
    }).join("");
  }

  // Rellena el modal de detalle con la ficha del producto y sus movimientos
  function renderDetalleProducto(codigo) {
    var p = db.productos.find(function (x) { return x.codigo === codigo; });
    if (!p) return;

    document.getElementById("detCodigo").textContent = p.codigo;
    document.getElementById("detNombre").textContent = p.nombre;

    var est = estadoDe(p);
    document.getElementById("detMeta").innerHTML =
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Categoría</span><strong>' + tramaEscapeHtml(p.categoria) + "</strong></div></div>" +
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Talla</span><strong>' + tramaEscapeHtml(p.talla) + "</strong></div></div>" +
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Color</span><strong><span class="swatch" style="background:' + tramaEscapeHtml(p.hex) + '"></span> ' + tramaEscapeHtml(p.color) + "</strong></div></div>" +
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Ubicación</span><strong>' + tramaEscapeHtml(p.ubicacion) + "</strong></div></div>" +
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Stock actual</span><strong>' + tramaFormatNumber(p.stock) + " uds.</strong></div></div>" +
      '<div class="col-sm-6 col-lg-3"><div class="detalle-item"><span>Stock mínimo</span><strong>' + tramaFormatNumber(p.minimo) + " uds.</strong></div></div>";

    document.getElementById("detEstado").innerHTML = tramaBadge(est.label, est.cls);

    var pct = Math.min(100, Math.round((p.stock / Math.max(p.minimo * 2, 1)) * 100));
    var bar = document.getElementById("detBarra");
    bar.style.width = pct + "%";
    bar.className = "progress-bar " + (est.key === "out" ? "bg-danger" : est.key === "low" ? "bg-warning" : "bg-success");

    var movs = db.movimientos.filter(function (m) { return m.codigo === codigo; });
    var tbody = document.getElementById("detMovimientos");
    if (movs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted-soft py-4">Este producto aún no tiene movimientos.</td></tr>';
    } else {
      tbody.innerHTML = movs.map(function (m) {
        return "<tr>" +
          '<td class="text-muted-soft">' + tramaEscapeHtml(m.fecha) + "</td>" +
          "<td>" + tramaBadge(m.tipo === "entrada" ? "Entrada" : "Salida", m.tipo === "entrada" ? "badge-ok" : "badge-transit") + "</td>" +
          "<td>" + tramaFormatNumber(m.cantidad) + "</td>" +
          '<td class="text-muted-soft">' + tramaEscapeHtml(m.origen) + "</td>" +
          '<td class="text-muted-soft">' + tramaEscapeHtml(m.responsable) + "</td>" +
          "</tr>";
      }).join("");
    }

    bootstrap.Modal.getOrCreateInstance(document.getElementById("modalDetalleProducto")).show();
  }

  function initInventario() {
    document.getElementById("buscador").addEventListener("input", renderInventario);
    document.getElementById("filtroCategoria").addEventListener("change", renderInventario);
    document.getElementById("filtroEstado").addEventListener("change", renderInventario);

    document.getElementById("tablaInventario").addEventListener("click", function (e) {
      var btn = e.target.closest("[data-view-product]");
      if (btn) renderDetalleProducto(btn.dataset.viewProduct);
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
      tramaToast("Producto registrado correctamente.", "success");
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