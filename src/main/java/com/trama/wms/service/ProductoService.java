package com.trama.wms.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.model.Producto;
import com.trama.wms.repository.ProductoRepository;

/**
 * El id_producto lo genera MySQL (AUTO_INCREMENT) y el codigo visible
 * (TR-1001, TR-1002...) lo genera el sistema: el usuario ya no lo escribe.
 */
@Service
public class ProductoService {

    private static final int PRIMER_NUMERO = 1001;

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    /** Siguiente codigo libre: toma el mayor TR-#### existente y le suma 1. */
    public String siguienteCodigo() {
        int mayor = repository.codigosTr().stream()
                .map(c -> c.substring(3))
                .filter(n -> n.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(PRIMER_NUMERO - 1);
        return String.format("TR-%04d", mayor + 1);
    }

    @Transactional
    public Producto guardar(Producto producto) {
        if (producto.getIdProducto() == null) {
            producto.setCodigo(siguienteCodigo());          // nuevo: codigo automatico
        } else {
            // editar: se conserva el codigo original aunque alguien lo cambie en el formulario
            repository.findById(producto.getIdProducto())
                    .ifPresent(actual -> producto.setCodigo(actual.getCodigo()));
        }
        return repository.save(producto);
    }

}
