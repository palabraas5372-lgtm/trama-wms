package com.trama.wms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Solicitud;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    List<Solicitud> findAllByOrderByFechaDesc();

    long countByEstado(String estado);

    long countByUsuario_IdUsuario(Integer idUsuario);

}
