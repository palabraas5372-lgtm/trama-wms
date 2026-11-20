package com.trama.wms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trama.wms.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    /** Productos cuyo stock ya quedo en o por debajo del minimo, del mas critico al menos. */
    @Query("select p from Producto p where p.stock <= p.stockMinimo order by p.stock asc, p.nombre asc")
    List<Producto> findRequierenReposicion();

    @Query("select p.categoria.nombre, sum(p.stock) from Producto p group by p.categoria.nombre order by p.categoria.nombre")
    List<Object[]> unidadesPorCategoria();


    /** Codigos con formato TR-#### para calcular el siguiente. */
    @Query("select p.codigo from Producto p where p.codigo like 'TR-%'")
    List<String> codigosTr();

    /** Catalogo del portal cliente: filtro opcional por categoria y por texto. */
    @Query("""
            select p from Producto p
            where (:idCategoria is null or p.categoria.idCategoria = :idCategoria)
              and (:texto is null or lower(p.nombre) like concat('%', :texto, '%')
                                  or lower(p.codigo) like concat('%', :texto, '%'))
            order by p.categoria.nombre, p.nombre, p.talla
            """)
    List<Producto> buscarCatalogo(@Param("idCategoria") Integer idCategoria, @Param("texto") String texto);

}
