package com.trama.wms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.dto.ReporteMes;
import com.trama.wms.model.Venta;
import com.trama.wms.repository.VentaRepository;

/** Reporte de ventas por mes y por año, calculado sobre la tabla ventas. */
@Service
public class ReporteVentasService {

    private static final String[] MESES = { "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre" };

    private final VentaRepository ventaRepository;

    public ReporteVentasService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    /** Años que tienen ventas, más el actual (para la lista desplegable). */
    @Transactional(readOnly = true)
    public List<Integer> anios() {
        TreeSet<Integer> anios = new TreeSet<>();
        anios.add(LocalDate.now().getYear());
        ventaRepository.findAll().forEach(v -> anios.add(v.getFecha().getYear()));
        return new ArrayList<>(anios.descendingSet());
    }

    /** Las 12 filas (enero a diciembre) de un año. */
    @Transactional(readOnly = true)
    public List<ReporteMes> porMes(int anio) {
        List<Venta> ventas = ventaRepository.findAll().stream()
                .filter(v -> v.getFecha().getYear() == anio).toList();
        List<ReporteMes> filas = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            final int m = mes;
            List<Venta> delMes = ventas.stream().filter(v -> v.getFecha().getMonthValue() == m).toList();
            filas.add(fila(anio, mes, MESES[mes - 1], delMes));
        }
        return filas;
    }

    /** Una fila por año con el acumulado. */
    @Transactional(readOnly = true)
    public List<ReporteMes> porAnio() {
        List<Venta> todas = ventaRepository.findAll();
        List<ReporteMes> filas = new ArrayList<>();
        for (Integer anio : anios()) {
            List<Venta> delAnio = todas.stream().filter(v -> v.getFecha().getYear() == anio).toList();
            filas.add(fila(anio, 0, String.valueOf(anio), delAnio));
        }
        return filas;
    }

    public static ReporteMes totalDe(List<ReporteMes> filas, String etiqueta) {
        return new ReporteMes(0, 0, etiqueta,
                filas.stream().mapToInt(ReporteMes::ventas).sum(),
                filas.stream().mapToInt(ReporteMes::unidades).sum(),
                filas.stream().map(ReporteMes::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                filas.stream().map(ReporteMes::descuentos).reduce(BigDecimal.ZERO, BigDecimal::add),
                filas.stream().map(ReporteMes::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static ReporteMes fila(int anio, int mes, String nombre, List<Venta> ventas) {
        return new ReporteMes(anio, mes, nombre,
                ventas.size(),
                ventas.stream().mapToInt(Venta::getTotalUnidades).sum(),
                ventas.stream().map(Venta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                ventas.stream().map(Venta::getDescuento).reduce(BigDecimal.ZERO, BigDecimal::add),
                ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
