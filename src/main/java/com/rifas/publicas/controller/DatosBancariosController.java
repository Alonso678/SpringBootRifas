package com.rifas.publicas.controller;

import com.rifas.publicas.model.DatosBancarios;
import com.rifas.publicas.model.Usuario;
import com.rifas.publicas.repository.DatoBancarioRepository;
import com.rifas.publicas.repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DatosBancariosController {

    private final DatoBancarioRepository datoBancarioRepository;
    private final UsuarioRepository usuarioRepository;

    public DatosBancariosController(DatoBancarioRepository datoBancarioRepository, UsuarioRepository usuarioRepository) {
        this.datoBancarioRepository = datoBancarioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/super-admin/usuarios/bancos")
    public String guardarDatosBancarios(@RequestParam("usuarioId") Long usuarioId,
            @RequestParam("banco") String banco,
            @RequestParam("titularCuenta") String titularCuenta,
            @RequestParam("clabeInterbancaria") String clabeInterbancaria,
            RedirectAttributes redirectAttributes) {
        try {
            // Verificar si el usuario es administrador antes de persistir
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!"ROLE_ADMIN".equals(usuario.getRol())) {
                redirectAttributes.addFlashAttribute("mensajeError", "El usuario seleccionado no cuenta con rol de Administrador.");
                return "redirect:/super-admin/usuarios";
            }

            // Buscar si ya existen datos bancarios para este usuario o crear uno nuevo
            DatosBancarios datos = datoBancarioRepository.findByUsuarioId(usuarioId)
                    .orElse(new DatosBancarios());

            datos.setUsuario(usuario);
            datos.setBanco(banco);
            datos.setTitularCuenta(titularCuenta);
            datos.setClabeInterbancaria(clabeInterbancaria);

            datoBancarioRepository.save(datos);

            redirectAttributes.addFlashAttribute("mensajeExito", "Datos bancarios guardados correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al guardar los datos bancarios: " + e.getMessage());
        }
        return "redirect:/super-admin/usuarios";
    }
}