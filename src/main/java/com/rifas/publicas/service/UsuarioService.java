package com.rifas.publicas.service;

import com.rifas.publicas.model.Usuario;
import com.rifas.publicas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> buscarUsuarios(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return usuarioRepository.findByEmailContainingIgnoreCase(termino);
    }

    @Transactional
    public void cambiarRolUsuario(Long idUsuario, String nuevoRol) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        // Protección estricta para evitar que el Super Administrador (ID 1) pierda su acceso accidentalmente
        if (usuario.getId().equals(1L) && !nuevoRol.equals("ROLE_SUPER_ADMIN")) {
            throw new IllegalArgumentException("No se permite modificar el rol del Super Administrador principal.");
        }

        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
    }
}
