package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.model.Producto;
import com.trama.wms.repository.ProductoRepository;
import com.trama.wms.service.ProductoService;

/**
 * El modulo de inventario vive embebido en index.html, por lo que este controller
 * solo recibe el alta/edicion y la eliminacion. El listado lo carga AppController.
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoRepository repository;
    private final ProductoService productoService;

    public ProductoController(ProductoRepository repository, ProductoService productoService) {
        this.repository = repository;
        this.productoService = productoService;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto, RedirectAttributes atributos) {
        boolean nuevo = producto.getIdProducto() == null;
        Producto guardado = productoService.guardar(producto);
        atributos.addFlashAttribute("mensaje", nuevo
                ? "Producto registrado con el código " + guardado.getCodigo() + "."
                : "Producto " + guardado.getCodigo() + " actualizado.");
        atributos.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/index#productos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repository.deleteById(id);
        return "redirect:/index#productos";
    }
}
