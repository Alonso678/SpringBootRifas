package com.rifas.publicas.controller;

import com.rifas.publicas.model.Usuario;
import com.rifas.publicas.service.UsuarioService;
import com.rifas.publicas.analisis.service.AdminAnalisisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    private final UsuarioService usuarioService;
    private final AdminAnalisisService adminAnalisisService;

    public SuperAdminController(UsuarioService usuarioService, AdminAnalisisService adminAnalisisService) {
        this.usuarioService = usuarioService;
        this.adminAnalisisService = adminAnalisisService;
    }

    // 1. Dashboard de Análisis Global
    @GetMapping("/analisis")
    public String verAnalisisGlobal(Model model) {
        Map<String, Object> estadisticas = adminAnalisisService.obtenerAnalisisGlobal();

        // Extraemos la lista de detalles rifas del mapa y la pasamos suelta al modelo
        Object detallesRifas = estadisticas.get("detallesRifas");

        model.addAttribute("estadisticas", estadisticas);
        model.addAttribute("detallesRifas", detallesRifas);

        return "super-admin-analisis"; // Nombre del archivo HTML que crearemos en la Fase 4
    }

    // 2. Lista de Usuarios (Actualizado con soporte para listar todos y filtros avanzados por rol)
    @GetMapping("/usuarios")
    public String listarUsuarios(
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "listar", required = false) String listar,
            @RequestParam(value = "rolFiltro", required = false) String rolFiltro,
            Model model) {

        List<Usuario> usuarios;

        if ("todos".equals(listar)) {
            // Opción para traer a todos ordenados del más reciente al más antiguo
            usuarios = usuarioService.listarTodosOrdenadosPorRecientes(); 
            model.addAttribute("terminoBusqueda", "Todos los usuarios");
        } else if (buscar != null && !buscar.trim().isEmpty()) {
            // Llama al repositorio usando el término ingresado
            usuarios = usuarioService.buscarUsuarios(buscar);
            model.addAttribute("terminoBusqueda", buscar);
        } else if (rolFiltro != null && !rolFiltro.trim().isEmpty()) {
            // Filtro avanzado por rol específico (ej. administradores o usuarios normales)
            usuarios = usuarioService.buscarPorRol(rolFiltro);
            model.addAttribute("terminoBusqueda", "Rol: " + rolFiltro);
        } else {
            // Si entras por primera vez o limpias, no carga nada
            usuarios = java.util.Collections.emptyList();
            model.addAttribute("terminoBusqueda", null);
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("rolFiltro", rolFiltro);

        return "super-admin-usuarios";
    }

    // 3. Modificar el Rol de un Usuario
    @PostMapping("/usuarios/{id}/cambiar-rol")
    public String cambiarRol(@PathVariable Long id, @RequestParam String nuevoRol,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarRolUsuario(id, nuevoRol);
            redirectAttributes.addFlashAttribute("mensajeExito", "Rol actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage()); // Captura el error de proteger al ID
                                                                                  // 1
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Ocurrió un error al actualizar el rol.");
        }
        return "redirect:/super-admin/usuarios";
    }
}