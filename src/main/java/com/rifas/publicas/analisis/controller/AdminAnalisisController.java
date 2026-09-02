package com.rifas.publicas.analisis.controller;

import com.rifas.publicas.analisis.model.AnalisisDashboardDTO;
import com.rifas.publicas.analisis.service.AdminAnalisisService;
import com.rifas.publicas.model.Rifa;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/analisis")
public class AdminAnalisisController {

    private final AdminAnalisisService analisisService;

    AdminAnalisisController(AdminAnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @GetMapping
    public String mostrarDashboardAnalisis(@RequestParam(required = false) Long rifaId, Model model) {
        List<Rifa> listaRifas = analisisService.obtenerTodasLasRifas();
        model.addAttribute("rifas", listaRifas);

        // Agrega esta línea para que el modal de la rifa disponga del objeto requerido
        if (!model.containsAttribute("nuevaRifa")) {
            model.addAttribute("nuevaRifa", new Rifa());
        }
        
        if (!listaRifas.isEmpty()) {
            // Si no viene un ID por parámetro, se selecciona la primera por defecto
            Long idSeleccionado = (rifaId != null) ? rifaId : listaRifas.get(0).getId();

            AnalisisDashboardDTO metricas = analisisService.calcularMetricasRifa(idSeleccionado);
            model.addAttribute("metricas", metricas);
            model.addAttribute("rifaSeleccionadaId", idSeleccionado); // Crucial para que th:selected marque la opción
                                                                      // activa
        }

        return "admin-analisis";
    }
}
