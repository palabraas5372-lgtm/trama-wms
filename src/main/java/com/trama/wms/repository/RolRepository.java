package com.trama.wms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
    
}