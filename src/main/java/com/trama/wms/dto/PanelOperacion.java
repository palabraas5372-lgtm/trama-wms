package com.trama.wms.dto;

import java.util.List;

/**
 * Cifras del panel de control calculadas sobre las tablas de movimientos y
 * productos: unidades en stock, flujo de la ultima semana y series para los graficos.
 */
public record PanelOperacion(int unidadesEnStock, int productosPorReponer, int movimientosSemana, int entradasSemana,
        int salidasSemana, int unidadesEntradasSemana, int unidadesSalidasSemana, List<String> diasLabels,
        List<Integer> unidadesEntradaDia, List<Integer> unidadesSalidaDia, List<String> rotacionLabels,
        List<Integer> rotacionValores) {

}
