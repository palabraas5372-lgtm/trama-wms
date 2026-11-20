package com.trama.wms.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Descuentos por volumen para los clientes (venta al por mayor). Se calculan
 * sobre el TOTAL de unidades del pedido. El último nivel es el descuento especial
 * para clientes que abastecen sus propias tiendas.
 */
public final class Descuento {

    public record Nivel(int minimoUnidades, int porcentaje, String nombre) {
    }

    /** De menor a mayor. */
    public static final List<Nivel> NIVELES = List.of(
            new Nivel(50, 5, "Mayorista"),
            new Nivel(100, 10, "Mayorista plus"),
            new Nivel(200, 15, "Especial para tiendas"));

    /**
     * Descuento adicional por fechas especiales del Perú (campañas textiles más
     * importantes). Se SUMA al descuento por volumen y se fija al crear el pedido.
     */
    public record Festivo(String nombre, int porcentaje, LocalDate desde, LocalDate hasta, String icono) {
        public boolean incluye(LocalDate dia) {
            return !dia.isBefore(desde) && !dia.isAfter(hasta);
        }
    }

    /** Campañas de un año: Año Nuevo, Día de la Madre, Fiestas Patrias y Navidad. */
    public static List<Festivo> festivos(int anio) {
        // Día de la Madre en el Perú: segundo domingo de mayo
        LocalDate diaMadre = LocalDate.of(anio, 5, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
        return List.of(
                new Festivo("Año Nuevo", 5, LocalDate.of(anio, 1, 1), LocalDate.of(anio, 1, 2), "bi-stars"),
                new Festivo("Día de la Madre", 5, diaMadre.minusDays(7), diaMadre, "bi-heart"),
                new Festivo("Fiestas Patrias", 8, LocalDate.of(anio, 7, 20), LocalDate.of(anio, 7, 29), "bi-flag"),
                new Festivo("Navidad", 10, LocalDate.of(anio, 12, 15), LocalDate.of(anio, 12, 25), "bi-gift"),
                new Festivo("Año Nuevo", 5, LocalDate.of(anio, 12, 26), LocalDate.of(anio, 12, 31), "bi-stars"));
    }

    /** Campaña vigente en un día, o null si no hay ninguna. */
    public static Festivo festivoVigente(LocalDate dia) {
        return festivos(dia.getYear()).stream().filter(f -> f.incluye(dia)).findFirst().orElse(null);
    }

    /** Próxima campaña a partir de un día (para anunciarla al cliente). */
    public static Festivo proximoFestivo(LocalDate dia) {
        return java.util.stream.Stream.concat(festivos(dia.getYear()).stream(), festivos(dia.getYear() + 1).stream())
                .filter(f -> f.desde().isAfter(dia)).findFirst().orElse(null);
    }

    private Descuento() {
    }

    /** Porcentaje que corresponde a una cantidad de unidades (0 si no llega al primer nivel). */
    public static int porcentaje(int unidades) {
        int pct = 0;
        for (Nivel n : NIVELES) {
            if (unidades >= n.minimoUnidades()) {
                pct = n.porcentaje();
            }
        }
        return pct;
    }

    /** Siguiente nivel a alcanzar, o null si ya tiene el máximo. */
    public static Nivel siguiente(int unidades) {
        for (Nivel n : NIVELES) {
            if (unidades < n.minimoUnidades()) {
                return n;
            }
        }
        return null;
    }

    public static BigDecimal monto(BigDecimal subtotal, int porcentaje) {
        return subtotal.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
