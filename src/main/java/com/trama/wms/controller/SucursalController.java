package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.trama.wms.model.Sucursal;
import com.trama.wms.repository.SucursalRepository;

/** Solo mutaciones: el listado lo carga AppController en index.html. */
@Controller
@RequestMapping("/sucursales")
public class SucursalController {

    private final SucursalRepository repository;

    public SucursalController(SucursalRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Sucursal sucursal) {
        repository.save(sucursal);
        return "redirect:/index#sucursales";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repository.deleteById(id);
        return "redirect:/index#sucursales";
    }
}