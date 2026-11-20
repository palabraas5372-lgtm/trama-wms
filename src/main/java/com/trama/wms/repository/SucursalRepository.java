package com.trama.wms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Sucursal;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    long countByEstado(String estado);

    List<Sucursal> findAllByOrderByNombreAsc();

    /** Almacenes que el cliente puede elegir al registrarse (solo los activos). */
    List<Sucursal> findByEstadoOrderByNombreAsc(String estado);

}
