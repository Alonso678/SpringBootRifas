package com.rifas.publicas.analisis.controller;

import com.rifas.publicas.analisis.model.AnalisisDashboardDTO;
import com.rifas.publicas.analisis.service.AdminAnalisisService;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.model.Usuario;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.repository.UsuarioRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/analisis")
public class AdminAnalisisController {

    private final AdminAnalisisService analisisService;
    private final RifaRepository rifaRepository; // Inyecta el repositorio o ajústalo a tu servicio
    private final UsuarioRepository usuarioRepository;

    AdminAnalisisController(AdminAnalisisService analisisService, RifaRepository rifaRepository,
            UsuarioRepository usuarioRepository) {
        this.analisisService = analisisService;
        this.rifaRepository = rifaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String mostrarDashboardAnalisis(@RequestParam(required = false) Long rifaId, Model model, Principal principal) {
        // Obtiene el ID real del administrador autenticado a partir de su sesión
        Long adminId = obtenerIdAdministradorActual(principal);

        if (adminId == null) {
            return "redirect:/login";
        }

        // Ejecuta la verificación y actualización masiva de estados vencidos
        analisisService.verificarYActualizarRifasVencidas(adminId);

        // Filtra únicamente las rifas activas que pertenecen a este administrador ID[cite: 16, 17]
        List<Rifa> listaRifas = rifaRepository.findByAdministradorIdAndEstado(adminId, "ACTIVA");
        model.addAttribute("rifas", listaRifas);

        if (!model.containsAttribute("nuevaRifa")) {
            model.addAttribute("nuevaRifa", new Rifa());
        }

        if (!listaRifas.isEmpty()) {
            Long idSeleccionado = (rifaId != null) ? rifaId : listaRifas.get(0).getId();

            AnalisisDashboardDTO metricas = analisisService.calcularMetricasRifa(idSeleccionado);
            model.addAttribute("metricas", metricas);
            model.addAttribute("rifaSeleccionadaId", idSeleccionado);
        } else {
            model.addAttribute("metricas", null);
            model.addAttribute("rifaSeleccionadaId", null);
        }

        return "admin-analisis";
    }

    @SuppressWarnings("null")
    private Long obtenerIdAdministradorActual(Principal principal) {
        if (principal == null) {
            return null;
        }
        // principal.getName() obtiene el correo con el que inició sesión
        String email = principal.getName();
        
        // Como findByEmail retorna Optional<Usuario>, extraemos el ID de forma segura
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId)
                .orElse(null);
    }
}
