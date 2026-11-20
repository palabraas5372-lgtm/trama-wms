// Gráficos del dashboard. Los datos llegan desde AppController en window.TRAMA_CHARTS
// (calculados sobre las tablas de movimientos y productos) y los gráficos se crean la
// primera vez que se muestra el módulo, y se recrean si se vuelve a entrar para no
// dejar canvas en blanco.

(function () {
  var graficos = {};

  function datos() {
    return window.TRAMA_CHARTS || {};
  }

  function pintar(id, configuracion) {
    var lienzo = document.getElementById(id);
    if (!lienzo) return;

    if (graficos[id]) {
      graficos[id].destroy();
      delete graficos[id];
    }
    graficos[id] = new Chart(lienzo, configuracion);
  }

  function ejesCategoria(horizontal) {
    var comun = { grid: { display: false }, ticks: { font: { family: 'Inter', size: 11 } } };
    var unidades = {
      beginAtZero: true,
      grid: { color: 'rgba(27,31,42,.06)' },
      ticks: { font: { family: 'Inter', size: 11 } }
    };
    return horizontal ? { x: unidades, y: comun } : { x: comun, y: unidades };
  }

  function render() {
    if (!window.Chart) return;

    var d = datos();

    // Flujo diario: unidades que entraron y que salieron en cada uno de los 7 días
    pintar('chartFlujo', {
      type: 'bar',
      data: {
        labels: d.diasLabels || [],
        datasets: [
          {
            label: 'Ingresadas',
            data: d.entradaPorDia || [],
            backgroundColor: '#3F8F6F',
            borderRadius: 6,
            maxBarThickness: 26
          },
          {
            label: 'Despachadas',
            data: d.salidaPorDia || [],
            backgroundColor: '#2F4771',
            borderRadius: 6,
            maxBarThickness: 26
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { font: { family: 'Inter', size: 12 }, usePointStyle: true, padding: 16 }
          }
        },
        scales: ejesCategoria(false)
      }
    });

    // Salud del inventario segun el stock minimo de cada producto
    pintar('chartEstados', {
      type: 'doughnut',
      data: {
        labels: ['En stock', 'Stock bajo', 'Agotado'],
        datasets: [{
          data: [d.inventarioOk || 0, d.inventarioBajo || 0, d.inventarioAgotado || 0],
          backgroundColor: ['#3F8F6F', '#E8A33D', '#C1483B'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { font: { family: 'Inter', size: 12 }, usePointStyle: true, padding: 16 }
          }
        }
      }
    });

    // Unidades disponibles agrupadas por categoría
    pintar('chartCategorias', {
      type: 'bar',
      data: {
        labels: d.categoriasLabels || [],
        datasets: [{
          label: 'Unidades en stock',
          data: d.categoriasValores || [],
          backgroundColor: '#2F4771',
          borderRadius: 6,
          maxBarThickness: 42
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: ejesCategoria(false)
      }
    });

    // Productos que mas unidades movieron en la semana
    pintar('chartRotacion', {
      type: 'bar',
      data: {
        labels: d.rotacionLabels || [],
        datasets: [{
          label: 'Unidades movidas',
          data: d.rotacionValores || [],
          backgroundColor: '#E8A33D',
          borderRadius: 6,
          maxBarThickness: 22
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: ejesCategoria(true)
      }
    });
  }

  window.tramaRenderCharts = render;
})();
