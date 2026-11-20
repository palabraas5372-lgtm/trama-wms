// Modulo de entradas y salidas: arma el detalle del movimiento en el modal.
//
// El usuario elige un producto y una cantidad, y cada linea se acumula en una tabla
// que muestra el stock actual, la cantidad a mover y el stock que quedara al guardar.
// Los campos ocultos productoIds y cantidades viajan al servidor, que es quien
// ajusta el stock real de la tabla productos.

(function () {

  var MODAL = 'modalMovimiento';

  var items = [];          // { productoId, cantidad }
  var originales = {};     // productoId -> cantidad que movio el registro original
  var tipoOriginal = null; // tipo del registro original (solo al editar)
  var catalogo = {};       // idProducto -> { codigo, nombre, variante, stock, minimo }

  // ══════════════ Utilidades ══════════════

  function modal() {
    return document.getElementById(MODAL);
  }

  function campo(selector) {
    var raiz = modal();
    return raiz ? raiz.querySelector(selector) : null;
  }

  function esEntrada() {
    var tipo = campo('#mTipo');
    return !!tipo && tipo.value === 'entrada';
  }

  function totalUnidades() {
    return items.reduce(function (suma, item) { return suma + item.cantidad; }, 0);
  }

  function buscarItem(idProducto) {
    for (var i = 0; i < items.length; i++) {
      if (items[i].productoId === idProducto) return items[i];
    }
    return null;
  }

  function mostrarError(mensaje) {
    var caja = campo('#mError');
    if (!caja) return;
    caja.textContent = mensaje || '';
    caja.classList.toggle('d-none', !mensaje);
  }

  function escapar(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // ══════════════ Catalogo de productos ══════════════

  // Se lee del propio <select> que el servidor renderizo, de modo que el modal
  // siempre ofrece el stock real sin pedirlo por HTTP.
  function cargarCatalogo() {
    catalogo = {};
    var select = campo('#mProducto');
    if (!select) return;

    Array.prototype.forEach.call(select.options, function (opcion) {
      if (!opcion.value) return;
      var color = opcion.getAttribute('data-color');
      var talla = opcion.getAttribute('data-talla');
      catalogo[opcion.value] = {
        codigo: opcion.getAttribute('data-codigo'),
        nombre: opcion.getAttribute('data-nombre'),
        variante: [color, talla].filter(Boolean).join(' / ') || '—',
        stock: parseInt(opcion.getAttribute('data-stock'), 10) || 0,
        minimo: parseInt(opcion.getAttribute('data-minimo'), 10) || 0
      };
    });
  }

  /**
   * Stock del producto antes del movimiento. Al crear uno nuevo es el stock actual;
   * al editar hay que descontar el efecto que ya tiene registrado, porque ese stock
   * todavia lo incluye.
   */
  function stockBase(producto, productoId) {
    var original = originales[productoId];
    if (!original || !tipoOriginal) return producto.stock;
    return producto.stock - (tipoOriginal === 'entrada' ? original : -original);
  }

  // ══════════════ Detalle del movimiento ══════════════

  function agregarProducto() {
    var select = campo('#mProducto');
    var entrada = campo('#mCantidad');
    mostrarError('');

    var producto = select ? catalogo[select.value] : null;
    if (!producto) {
      mostrarError('Selecciona un producto del catálogo.');
      return;
    }

    var cantidad = parseInt(entrada ? entrada.value : '0', 10) || 0;
    if (cantidad <= 0) {
      mostrarError('La cantidad debe ser mayor que cero.');
      return;
    }

    var idProducto = parseInt(select.value, 10);
    var existente = buscarItem(idProducto);
    if (existente) {
      // Si el producto ya estaba en el detalle, la cantidad se acumula en la misma linea
      existente.cantidad += cantidad;
    } else {
      items.push({ productoId: idProducto, cantidad: cantidad });
    }

    if (entrada) entrada.value = 1;
    if (select) select.selectedIndex = 0;
    renderDetalle();
  }

  function cambiarCantidad(indice, cantidad) {
    var item = items[indice];
    if (!item) return;

    mostrarError('');
    if (!cantidad || cantidad <= 0) {
      items.splice(indice, 1);
    } else {
      item.cantidad = cantidad;
    }
    renderDetalle();
  }

  function quitarProducto(indice) {
    items.splice(indice, 1);
    mostrarError('');
    renderDetalle();
  }

  function renderDetalle() {
    var cuerpo = campo('#mDetalleCuerpo');
    var envoltorio = campo('#mDetalleWrap');
    var vacio = campo('#mDetalleVacio');
    if (!cuerpo || !envoltorio || !vacio) return;

    var salida = !esEntrada();
    var filas = '';

    items.forEach(function (item, indice) {
      var producto = catalogo[item.productoId];
      if (!producto) return;

      var base = stockBase(producto, item.productoId);
      var resultante = Math.max(0, base + (salida ? -item.cantidad : item.cantidad));

      filas += '<tr>' +
        '<input type="hidden" name="productoIds" value="' + item.productoId + '">' +
        '<input type="hidden" name="cantidades" value="' + item.cantidad + '">' +
        '<td><span class="fw-semibold">' + escapar(producto.codigo) + '</span> ' +
        escapar(producto.nombre) +
        '<div class="text-muted-soft small">' + escapar(producto.variante) + '</div></td>' +
        '<td class="text-end text-nowrap">' + producto.stock + ' uds.</td>' +
        '<td><input type="number" class="form-control form-control-sm" min="1" step="1" ' +
        (salida ? 'max="' + base + '" ' : '') +
        'value="' + item.cantidad + '" data-indice="' + indice + '"></td>' +
        '<td class="text-end text-nowrap">' +
        '<span class="' + (salida ? 'delta-out' : 'delta-in') + ' fw-semibold">' +
        (salida ? '−' : '+') + item.cantidad + '</span>' +
        '<div class="text-muted-soft small">' + resultante + ' uds.</div></td>' +
        '<td class="text-end"><button type="button" class="btn btn-sm btn-outline-secondary" ' +
        'data-quitar="' + indice + '" title="Quitar del detalle">' +
        '<i class="bi bi-x-lg"></i></button></td>' +
        '</tr>';
    });

    cuerpo.innerHTML = filas;
    envoltorio.classList.toggle('d-none', items.length === 0);
    vacio.classList.toggle('d-none', items.length > 0);

    var resumen = campo('#mResumenDetalle');
    if (resumen) {
      resumen.textContent = items.length + (items.length === 1 ? ' producto · ' : ' productos · ')
        + totalUnidades() + ' unidades';
    }

    var total = campo('#mTotalUnidades');
    if (total) total.textContent = totalUnidades();

    var efectoTotal = campo('#mEfectoTotal');
    if (efectoTotal) {
      var saldo = totalUnidades();
      efectoTotal.textContent = salida ? 'Stock −' + saldo + ' uds.' : 'Stock +' + saldo + ' uds.';
    }
  }

  // ══════════════ Tipo de movimiento ══════════════

  // En una entrada se pide el proveedor y en una salida la sucursal destino.
  function aplicarTipo() {
    var entrada = esEntrada();
    var bloqueProveedor = campo('#mCampoProveedor');
    var bloqueSucursal = campo('#mCampoSucursal');
    var proveedor = campo('#mProveedor');
    var sucursal = campo('#mSucursal');

    if (bloqueProveedor) bloqueProveedor.classList.toggle('d-none', !entrada);
    if (bloqueSucursal) bloqueSucursal.classList.toggle('d-none', entrada);
    if (proveedor) proveedor.required = entrada;
    if (sucursal) sucursal.required = !entrada;

    renderDetalle();
  }

  // ══════════════ Apertura del modal ══════════════

  function ahora() {
    var fecha = new Date();
    function dos(digito) { return digito < 10 ? '0' + digito : String(digito); }
    return fecha.getFullYear() + '-' + dos(fecha.getMonth() + 1) + '-' + dos(fecha.getDate())
      + 'T' + dos(fecha.getHours()) + ':' + dos(fecha.getMinutes());
  }

  // Los selects se llenan siempre, aunque el dato venga vacio, para no arrastrar
  // el valor que hubiera quedado de la apertura anterior del modal.
  function fijar(selector, valor) {
    var elemento = campo(selector);
    if (elemento) elemento.value = valor == null ? '' : valor;
  }

  function preparar(modo, boton) {
    if (modo === 'editar' && boton) {
      fijar('#mProveedor', boton.getAttribute('data-proveedorid'));
      fijar('#mSucursal', boton.getAttribute('data-sucursalid'));
      fijar('#mUsuario', boton.getAttribute('data-usuarioid'));
      fijar('#mObservacion', boton.getAttribute('data-observacion'));
      fijar('#mFecha', boton.getAttribute('data-fecha'));

      // El detalle viaja serializado en el boton: [[idProducto, cantidad], ...]
      var detalle = [];
      try {
        detalle = JSON.parse(boton.getAttribute('data-detalle') || '[]');
      } catch (error) {
        detalle = [];
      }

      items = [];
      originales = {};
      tipoOriginal = boton.getAttribute('data-tipo');

      detalle.forEach(function (linea) {
        if (!linea || !linea[0] || !(linea[1] > 0)) return;
        items.push({ productoId: linea[0], cantidad: linea[1] });
        originales[linea[0]] = linea[1];
      });
    } else {
      items = [];
      originales = {};
      tipoOriginal = null;

      var tipo = campo('#mTipo');
      if (tipo && !tipo.value) tipo.value = 'entrada';
      var fecha = campo('#mFecha');
      if (fecha && !fecha.value) fecha.value = ahora();
      var cantidad = campo('#mCantidad');
      if (cantidad) cantidad.value = 1;
      var select = campo('#mProducto');
      if (select) select.selectedIndex = 0;
    }

    mostrarError('');
    aplicarTipo();
  }

  // ══════════════ Arranque ══════════════

  document.addEventListener('click', function (evento) {
    if (!evento.target.closest('#' + MODAL)) return;

    if (evento.target.closest('#mAgregar')) {
      evento.preventDefault();
      agregarProducto();
      return;
    }

    var quitar = evento.target.closest('[data-quitar]');
    if (quitar) {
      evento.preventDefault();
      quitarProducto(parseInt(quitar.getAttribute('data-quitar'), 10));
    }
  });

  document.addEventListener('change', function (evento) {
    var raiz = evento.target.closest('#' + MODAL);
    if (!raiz) return;

    if (evento.target.id === 'mTipo') {
      aplicarTipo();
      return;
    }

    // Cantidad editada en la fila del detalle: al repintar la tabla se refrescan
    // tambien los campos ocultos que viajan al servidor
    if (evento.target.matches('#mDetalleCuerpo input[type="number"]')) {
      cambiarCantidad(parseInt(evento.target.getAttribute('data-indice'), 10),
        parseInt(evento.target.value, 10));
    }
  });

  document.addEventListener('keydown', function (evento) {
    if (evento.key === 'Enter' && evento.target.id === 'mProducto') {
      evento.preventDefault();
      agregarProducto();
    }
  });

  // Antes de enviar se revisa el detalle: no se guarda un movimiento sin productos
  document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    if (!formulario || formulario.id !== 'formMovimiento') return;

    if (items.length === 0) {
      evento.preventDefault();
      mostrarError('Agrega al menos un producto con su cantidad.');
      return;
    }

    if (items.some(function (item) { return !item.cantidad || item.cantidad <= 0; })) {
      evento.preventDefault();
      mostrarError('Revisa las cantidades: todas deben ser mayores que cero.');
    }
  });

  document.addEventListener('DOMContentLoaded', function () {
    cargarCatalogo();
  });

  // Gancho que invoca trama-app.js antes de mostrar el modal
  window.tramaModalAbierto = function (idModal, modo, boton) {
    if (idModal !== MODAL) return;
    if (!Object.keys(catalogo).length) cargarCatalogo();
    preparar(modo, boton);
  };

})();
