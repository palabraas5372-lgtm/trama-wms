(function () {
  function poblarProductosSelect() {
    var select = document.getElementById("mProducto");
    select.innerHTML = db.productos.map(function (p) {
      return '<option value="' + p.codigo + '">' + p.codigo + ' — ' + p.nombre + ' (' + p.color + ', ' + p.talla + ')</option>';
    }).join("");
  }

  function renderHistorialMovimientos() {
    poblarProductosSelect();
    var filtro = document.getElementById("filtroTipo").value;
    var lista = db.movimientos.filter(function (m) { return !filtro || m.tipo === filtro; });
    var tbody = document.getElementById("tablaMovimientos");

    if (lista.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted-soft py-4">Sin movimientos registrados.</td></tr>';
      return;
    }

    tbody.innerHTML = lista.map(function (m) {
      return '<tr>' +
        '<td class="text-muted-soft">' + m.fecha + '</td>' +
        '<td><span class="badge-status ' + (m.tipo === "entrada" ? "badge-ok" : "badge-transit") + '">' + (m.tipo === "entrada" ? "Entrada" : "Salida") + '</span></td>' +
        '<td class="text-muted-soft">' + m.codigo + '</td>' +
        '<td>' + m.producto + '</td>' +
        '<td>' + m.cantidad + '</td>' +
        '<td class="text-muted-soft">' + m.origen + '</td>' +
        '<td class="text-muted-soft">' + m.responsable + '</td>' +
        '</tr>';
    }).join("");
  }

  function initMovimientos() {
    document.querySelectorAll('input[name="tipo"]').forEach(function (radio) {
      radio.addEventListener("change", function (e) {
        document.getElementById("lblOrigen").textContent = e.target.value === "entrada" ? "Proveedor de origen" : "Sucursal de destino";
        document.getElementById("mOrigen").placeholder = e.target.value === "entrada" ? "Ej. Proveedor Textil Andina" : "Ej. Sucursal Miraflores";
      });
    });

    document.getElementById("filtroTipo").addEventListener("change", renderHistorialMovimientos);

    document.getElementById("formMovimiento").addEventListener("submit", function (e) {
      e.preventDefault();
      var tipo = document.querySelector('input[name="tipo"]:checked').value;
      var codigo = document.getElementById("mProducto").value;
      var cantidad = Number(document.getElementById("mCantidad").value);
      var origen = document.getElementById("mOrigen").value;
      var responsable = document.getElementById("mResponsable").value;

      var producto = db.productos.find(function (p) { return p.codigo === codigo; });
      var errorBox = document.getElementById("movError");

      if (tipo === "salida" && cantidad > producto.stock) {
        errorBox.textContent = "Stock insuficiente. Disponible: " + producto.stock + " unidades.";
        errorBox.classList.remove("d-none");
        return;
      }
      errorBox.classList.add("d-none");

      producto.stock += tipo === "entrada" ? cantidad : -cantidad;

      var ahora = new Date();
      var fecha = ahora.toISOString().slice(0, 10) + " " + ahora.toTimeString().slice(0, 5);

      db.movimientos.unshift({
        fecha: fecha, tipo: tipo, codigo: codigo, producto: producto.nombre,
        cantidad: cantidad, origen: origen, responsable: responsable
      });

      tramaSaveDB(db);
      renderHistorialMovimientos();
      this.reset();
      document.getElementById("lblOrigen").textContent = "Proveedor de origen";
    });
  }

  window.TramaModules = window.TramaModules || {};
  window.TramaModules.movimientos = {
    renderHistorialMovimientos: renderHistorialMovimientos,
    initMovimientos: initMovimientos,
  };
})();
