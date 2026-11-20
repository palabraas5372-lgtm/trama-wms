package com.trama.wms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByUsuario_IdUsuarioOrderByFechaDesc(Integer idUsuario);

    List<Pedido> findAllByOrderByFechaDesc();

    long countByEstado(String estado);

    long countByUsuario_IdUsuario(Integer idUsuario);

}
