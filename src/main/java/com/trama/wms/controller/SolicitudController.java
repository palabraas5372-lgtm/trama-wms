package com.trama.wms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.trama.wms.model.Solicitud;
import com.trama.wms.repository.SolicitudRepository;

/** Solo mutaciones: el listado lo carga AppController en index.html. */
@Controller
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudRepository repository;

    public SolicitudController(SolicitudRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Solicitud solicitud) {
        repository.save(solicitud);
        return "redirect:/index#solicitudes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repository.deleteById(id);
        return "redirect:/index#solicitudes";
    }
}