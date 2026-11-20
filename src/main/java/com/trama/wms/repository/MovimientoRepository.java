package com.trama.wms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Movimiento;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    List<Movimiento> findAllByOrderByFechaDesc();

    @Query("select m from Movimiento m where m.fecha >= :desde order by m.fecha desc")
    List<Movimiento> findDesde(@Param("desde") LocalDateTime desde);

    long countByTipo(String tipo);

    long countByUsuario_IdUsuario(Integer idUsuario);

}
