package com.trama.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.DetalleMovimiento;

@Repository
public interface DetalleMovimientoRepository extends JpaRepository<DetalleMovimiento, Integer> {
    
}