package com.trama.wms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trama.wms.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findAllByOrderByFechaDesc();

    Optional<Venta> findByPedido_IdPedido(Integer idPedido);

    List<Venta> findByCliente_IdUsuarioOrderByFechaDesc(Integer idCliente);

    /** Último correlativo de la serie, para numerar el siguiente comprobante. */
    @Query("select coalesce(max(v.numero), 0) from Venta v where v.serie = :serie")
    Integer ultimoNumero(@Param("serie") String serie);

}
