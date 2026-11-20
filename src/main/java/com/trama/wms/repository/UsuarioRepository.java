package com.trama.wms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsuario(String usuario);

    long countByEstado(String estado);

    /* ── Validaciones del registro ───────────────────────────────── */

    boolean existsByDni(String dni);

    boolean existsByUsuarioIgnoreCase(String usuario);

    /** Para editar: el DNI esta tomado por OTRO usuario distinto al que se edita. */
    boolean existsByDniAndIdUsuarioNot(String dni, Integer idUsuario);

    boolean existsByUsuarioIgnoreCaseAndIdUsuarioNot(String usuario, Integer idUsuario);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdUsuarioNot(String telefono, Integer idUsuario);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Integer idUsuario);

    List<Usuario> findAllByOrderByIdUsuarioAsc();

}
