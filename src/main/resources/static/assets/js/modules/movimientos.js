// Módulo Movimientos: registro de entradas (proveedor) y salidas (sucursal)
// con control de stock, más historial filtrable. Se autoregistra en window.TramaModules.

(function () {
  function poblarProductosSelect() {
    var select = document.getElementById("mProducto");
    select.innerHTML = db.productos.map(function (p) {
      return '<option value="' + tramaEscapeHtml(p.codigo) + '">' + tramaEscapeHtml(p.codigo) + " — " + tramaEscapeHtml(p.nombre) + " (" + tramaEscapeHtml(p.color) + ", " + tramaEscapeHtml(p.talla) + ")</option>";
    }).join("");
  }

  function poblarSucursalesSelect() {
    var select = document.getElementById("mOrigenSelect");
    select.innerHTML = db.sucursales.map(function (s) {
      return '<option value="Sucursal ' + tramaEscapeHtml(s.nombre.replace("TRAMA ", "")) + '">' + tramaEscapeHtml(s.nombre) + " — " + tramaEscapeHtml(s.ciudad) + "</option>";
    }).join("");
  }

  // Alterna entre "Proveedor de origen" (entrada) y "Sucursal de destino" (salida)
  function aplicarTipoMovimiento(tipo) {
    var esEntrada = tipo === "entrada";
    var lbl = document.getElementById("lblOrigen");
    var input = document.getElementById("mOrigen");
    var select = document.getElementById("mOrigenSelect");

    lbl.textContent = esEntrada ? "Proveedor de origen" : "Sucursal de destino";
    input.classList.toggle("d-none", !esEntrada);
    input.required = esEntrada;
    select.classList.toggle("d-none", esEntrada);
    select.required = !esEntrada;
  }

  function renderHistorialMovimientos() {
    poblarProductosSelect();
    var filtro = document.getElementById("filtroTipo").value;
    var lista = db.movimientos.filter(function (m) { return !filtro || m.tipo === filtro; });
    var tbody = document.getElementById("tablaMovimientos");

    if (lista.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><i class="bi bi-arrow-left-right text-muted-soft" style="font-size:1.8rem;"></i><div class="text-muted-soft mt-2">Sin movimientos registrados.</div></td></tr>';
      return;
    }

    tbody.innerHTML = lista.map(function (m) {
      return "<tr>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.fecha) + "</td>" +
        "<td>" + tramaBadge(m.tipo === "entrada" ? "Entrada" : "Salida", m.tipo === "entrada" ? "badge-ok" : "badge-transit") + "</td>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.codigo) + "</td>" +
        "<td>" + tramaEscapeHtml(m.producto) + "</td>" +
        "<td>" + tramaFormatNumber(m.cantidad) + "</td>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.origen) + "</td>" +
        '<td class="text-muted-soft">' + tramaEscapeHtml(m.responsable) + "</td>" +
        "</tr>";
    }).join("");
  }

  function initMovimientos() {
    poblarSucursalesSelect();

    document.querySelectorAll('input[name="tipo"]').forEach(function (radio) {
      radio.addEventListener("change", function (e) {
        aplicarTipoMovimiento(e.target.value);
      });
    });
    aplicarTipoMovimiento("entrada");

    document.getElementById("filtroTipo").addEventListener("change", renderHistorialMovimientos);

    document.getElementById("formMovimiento").addEventListener("submit", function (e) {
      e.preventDefault();
      var tipo = document.querySelector('input[name="tipo"]:checked').value;
      var codigo = document.getElementById("mProducto").value;
      var cantidad = Number(document.getElementById("mCantidad").value);
      var esEntrada = tipo === "entrada";
      var origen = esEntrada
        ? document.getElementById("mOrigen").value
        : document.getElementById("mOrigenSelect").value;
      var responsable = document.getElementById("mResponsable").value;

      var producto = db.productos.find(function (p) { return p.codigo === codigo; });
      var errorBox = document.getElementById("movError");

      if (!producto) {
        errorBox.textContent = "Selecciona un producto válido.";
        errorBox.classList.remove("d-none");
        return;
      }

      if (tipo === "salida" && cantidad > producto.stock) {
        errorBox.textContent = "Stock insuficiente. Disponible: " + producto.stock + " unidades.";
        errorBox.classList.remove("d-none");
        return;
      }
      errorBox.classList.add("d-none");

      producto.stock += esEntrada ? cantidad : -cantidad;

      var ahora = new Date();
      var fecha = ahora.toISOString().slice(0, 10) + " " + ahora.toTimeString().slice(0, 5);

      db.movimientos.unshift({
        fecha: fecha, tipo: tipo, codigo: codigo, producto: producto.nombre,
        cantidad: cantidad, origen: origen, responsable: responsable
      });

      tramaSaveDB(db);
      renderHistorialMovimientos();
      this.reset();
      aplicarTipoMovimiento("entrada");
      tramaToast((esEntrada ? "Entrada registrada" : "Salida registrada") + " · " + cantidad + " uds. de " + producto.nombre, "success");
    });
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.movimientos = {
    renderHistorialMovimientos: renderHistorialMovimientos,
    initMovimientos: initMovimientos,
  };
})();