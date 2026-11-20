package com.trama.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.DetalleSolicitud;

@Repository
public interface DetalleSolicitudRepository extends JpaRepository<DetalleSolicitud, Integer> {
    
}