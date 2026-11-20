package com.trama.wms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Perfil;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    Optional<Perfil> findByNombreIgnoreCase(String nombre);

    List<Perfil> findAllByOrderByNombreAsc();

}
