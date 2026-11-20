package com.trama.wms.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.trama.wms.dto.UsuarioForm;
import com.trama.wms.repository.PerfilRepository;
import com.trama.wms.repository.SucursalRepository;
import com.trama.wms.service.UsuarioInvalidoException;
import com.trama.wms.service.UsuarioService;

/**
 * Registro publico de usuarios (solo creacion), enlazado desde el login.
 * La cuenta nace en estado "pendiente" y solo un administrador puede activarla.
 */
@Controller
@RequestMapping("/registro")
public class RegistroController {

    private final UsuarioService usuarioService;
    private final PerfilRepository perfilRepository;
    private final SucursalRepository sucursalRepository;

    public RegistroController(UsuarioService usuarioService, PerfilRepository perfilRepository,
            SucursalRepository sucursalRepository) {
        this.usuarioService = usuarioService;
        this.perfilRepository = perfilRepository;
        this.sucursalRepository = sucursalRepository;
    }

    /** Listas desplegables del formulario, cargadas desde la BD. */
    private void cargarListas(Model model) {
        model.addAttribute("perfilesL", perfilRepository.findAllByOrderByNombreAsc());
        model.addAttribute("almacenesL", sucursalRepository.findByEstadoOrderByNombreAsc("activa"));
    }

    @GetMapping
    public String formulario(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UsuarioForm());
        }
        cargarListas(model);
        return "registro";
    }

    @PostMapping
    public String registrar(@ModelAttribute("form") UsuarioForm form, Model model, RedirectAttributes atributos) {
        try {
            usuarioService.registrar(form);
        } catch (UsuarioInvalidoException error) {
            // Se vuelve a mostrar el formulario con lo ya escrito (menos la clave)
            form.setClave(null);
            model.addAttribute("error", error.getMessage());
            cargarListas(model);
            return "registro";
        }

        atributos.addFlashAttribute("exito",
                "¡Registro enviado! Tu cuenta quedó en estado PENDIENTE hasta que un administrador la active.");
        atributos.addFlashAttribute("usuarioIntentado", form.getUsuario());
        return "redirect:/";
    }

    /** Validacion en vivo del formulario: /registro/disponible?campo=dni&valor=12345678 */
    @GetMapping("/disponible")
    @ResponseBody
    public Map<String, Object> disponible(@RequestParam String campo, @RequestParam String valor) {
        boolean libre = switch (campo) {
            case "dni" -> usuarioService.dniDisponible(valor);
            case "usuario" -> usuarioService.usuarioDisponible(valor);
            case "telefono" -> usuarioService.telefonoDisponible(valor);
            case "correo" -> usuarioService.correoDisponible(valor);
            default -> true;
        };
        return Map.of("disponible", libre);
    }

}
