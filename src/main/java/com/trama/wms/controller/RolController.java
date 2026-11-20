package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.trama.wms.model.Rol;
import com.trama.wms.repository.RolRepository;

/** Solo mutaciones: el listado lo carga AppController en index.html. */
@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolRepository repository;

    public RolController(RolRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Rol rol) {
        repository.save(rol);
        return "redirect:/index#roles";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repository.deleteById(id);
        return "redirect:/index#roles";
    }
}