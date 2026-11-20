package com.trama.wms.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trama.wms.model.Usuario;
import com.trama.wms.repository.UsuarioRepository;


@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    
    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String usuario, String clave) {

        if (usuario == null || clave == null || usuario.isBlank() || clave.isBlank()) {
            return Optional.empty();
        }

        Optional<Usuario> encontrado = usuarioRepository.findByUsuario(usuario.trim());

        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuarioDb = encontrado.get();

        
        if (!encoder.matches(clave, usuarioDb.getClave())) {
            return Optional.empty();
        }

        return Optional.of(usuarioDb);
    }

    
    public String cifrar(String clave) {
        return encoder.encode(clave);
    }

}
