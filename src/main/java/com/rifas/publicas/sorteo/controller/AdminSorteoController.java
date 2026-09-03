package com.rifas.publicas.sorteo.controller;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.repository.RifaRepository;
import com.rifas.publicas.sorteo.service.SorteoService;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/admin/sorteos")
public class AdminSorteoController {

    private static final Logger log = LoggerFactory.getLogger(AdminSorteoController.class);
    
    private final RifaRepository rifaRepository;
    private final BoletoRepository boletoRepository;
    private final SorteoService sorteoService;

    AdminSorteoController(RifaRepository rifaRepository, BoletoRepository boletoRepository, SorteoService sorteoService) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
        this.sorteoService = sorteoService;
    }

    @GetMapping
    public String listarRifasParaSorteo(Model model) {
        List<Rifa> rifas = rifaRepository.findAll();
        model.addAttribute("rifas", rifas);
        return "sorteo/admin-sorteos-lista";
    }

    @GetMapping("/{id}")
    public String verPanelSorteo(@PathVariable Long id, HttpSession session, Model model) {
        session.removeAttribute("idsDescartados_" + id);
        cargarDatosPanel(id, 5, session, model);
        return "sorteo/admin-sorteo-panel";
    }

    /**
     * Valida el umbral mínimo antes de permitir el acceso al panel del sorteo.
     */
    @GetMapping("/iniciar-validacion/{id}")
    public String validarEIniciarSorteo(@PathVariable Long id) {
        boolean cumple = sorteoService.cumpleUmbralMinimo(id);
        if (cumple) {
            return "redirect:/admin/sorteos/" + id;
        } else {
            return "redirect:/admin/analisis?rifaId=" + id + "&errorPorcentaje=true";
        }
    }

    // Este método maneja la ejecución del sorteo en bloques, descartando boletos hasta llegar al intervalo especificado.
    @PostMapping("/{id}/ejecutar")
    public String ejecutarSorteoBloque(@PathVariable Long id, @RequestParam int intervalo, HttpSession session,
            Model model) {
        Rifa rifa = rifaRepository.findById(id).orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        // Candidatos son los que siguen disponibles (estado "PAGADO")
        List<Boleto> boletosPagados = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id, "PAGADO");

        String sessionKeyDescartados = "idsDescartados_" + id;
        @SuppressWarnings("unchecked")
        List<Long> idsDescartados = (List<Long>) session.getAttribute(sessionKeyDescartados);
        if (idsDescartados == null) {
            idsDescartados = new ArrayList<>();
        }

        Boleto ganadorBloque = null;
        Boleto ultimoDescartado = null;

        if (!boletosPagados.isEmpty()) {
            int indexAleatorio = new Random().nextInt(boletosPagados.size());
            Boleto seleccionado = boletosPagados.get(indexAleatorio);

            // Si se completa el intervalo, este es el ganador
            if (idsDescartados.size() == intervalo - 1) {
                ganadorBloque = seleccionado;
                ganadorBloque.setEstado("GANADOR");
                boletoRepository.save(ganadorBloque);

                // Persistir el estatus FINALIZADA de la rifa
                rifa.setEstado("FINALIZADA");
                rifaRepository.save(rifa);
                
                session.removeAttribute(sessionKeyDescartados); // Limpiamos sesión para el siguiente bloque
            } else {
                // De lo contrario, se descarta y se persiste en BD
                seleccionado.setEstado("DESCARTADO");
                boletoRepository.save(seleccionado);

                idsDescartados.add(seleccionado.getId());
                session.setAttribute(sessionKeyDescartados, idsDescartados);
                ultimoDescartado = seleccionado;
            }
        }

        // Construir la lista de descartados respetando estrictamente el orden de la
        // sesión
        List<Boleto> descartadosBloque = new ArrayList<>();
        List<Boleto> todosLosDescartadosBD = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id,
                "DESCARTADO");

        for (Long idDesc : idsDescartados) {
            for (Boleto b : todosLosDescartadosBD) {
                if (b.getId().equals(idDesc)) {
                    descartadosBloque.add(b);
                    break;
                }
            }
        }

        model.addAttribute("rifa", rifa);
        model.addAttribute("boletosPagados",
                boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id, "PAGADO"));
        model.addAttribute("descartadosBloque", descartadosBloque);
        model.addAttribute("ganadorBloque", ganadorBloque);
        model.addAttribute("ultimoDescartado", ultimoDescartado);
        model.addAttribute("numeroDescarteActual", idsDescartados.size());
        model.addAttribute("intervaloSeleccionado", intervalo);

        return "sorteo/admin-sorteo-panel";
    }

    private void cargarDatosPanel(Long id, int intervalo, HttpSession session, Model model) {
        Rifa rifa = rifaRepository.findById(id).orElseThrow(() -> new RuntimeException("Rifa no encontrada"));
        List<Boleto> boletosPagados = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id, "PAGADO");

        String sessionKeyDescartados = "idsDescartados_" + id;
        @SuppressWarnings("unchecked")
        List<Long> idsDescartados = (List<Long>) session.getAttribute(sessionKeyDescartados);
        if (idsDescartados == null) {
            idsDescartados = new ArrayList<>();
        }

        List<Boleto> descartadosBloque = new ArrayList<>();
        List<Boleto> todosLosDescartadosBD = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id,
                "DESCARTADO");

        for (Long idDesc : idsDescartados) {
            for (Boleto b : todosLosDescartadosBD) {
                if (b.getId().equals(idDesc)) {
                    descartadosBloque.add(b);
                    break;
                }
            }
        }

        model.addAttribute("rifa", rifa);
        model.addAttribute("boletosPagados", boletosPagados);
        model.addAttribute("descartadosBloque", descartadosBloque);
        model.addAttribute("intervaloSeleccionado", intervalo);
    }

    @PostMapping("/{id}/ejecutar-manual")
    public String ejecutarSorteoBloqueManual(@PathVariable Long id,
            @RequestParam int intervalo,
            @RequestParam Long boletoId,
            HttpSession session,
            Model model) {
        Rifa rifa = rifaRepository.findById(id).orElseThrow(() -> new RuntimeException("Rifa no encontrada"));

        // Buscar el boleto específico que se seleccionó manualmente
        Boleto seleccionado = boletoRepository.findById(boletoId)
                .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));

        // Validar que el boleto siga pagado (por si hay un doble clic accidental)
        if (!"PAGADO".equals(seleccionado.getEstado())) {
            return "redirect:/admin/sorteos/" + id + "?error=boleto_invalido";
        }

        String sessionKeyDescartados = "idsDescartados_" + id;
        @SuppressWarnings("unchecked")
        List<Long> idsDescartados = (List<Long>) session.getAttribute(sessionKeyDescartados);
        if (idsDescartados == null) {
            idsDescartados = new ArrayList<>();
        }

        Boleto ganadorBloque = null;
        Boleto ultimoDescartado = null;

        // Lógica del bloque (igual que el automático)
        if (idsDescartados.size() == intervalo - 1) {
            ganadorBloque = seleccionado;
            ganadorBloque.setEstado("GANADOR");
            boletoRepository.save(ganadorBloque);
            session.removeAttribute(sessionKeyDescartados);
        } else {
            seleccionado.setEstado("DESCARTADO");
            boletoRepository.save(seleccionado);

            idsDescartados.add(seleccionado.getId());
            session.setAttribute(sessionKeyDescartados, idsDescartados);
            ultimoDescartado = seleccionado;
        }

        // Construir la lista de descartados respetando el orden
        List<Boleto> descartadosBloque = new ArrayList<>();
        List<Boleto> todosLosDescartadosBD = boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id,
                "DESCARTADO");

        for (Long idDesc : idsDescartados) {
            for (Boleto b : todosLosDescartadosBD) {
                if (b.getId().equals(idDesc)) {
                    descartadosBloque.add(b);
                    break;
                }
            }
        }

        model.addAttribute("rifa", rifa);
        model.addAttribute("boletosPagados",
                boletoRepository.findByRifaIdAndEstadoOrderByNumeroBoletoAsc(id, "PAGADO"));
        model.addAttribute("descartadosBloque", descartadosBloque);
        model.addAttribute("ganadorBloque", ganadorBloque);
        model.addAttribute("ultimoDescartado", ultimoDescartado);
        model.addAttribute("numeroDescarteActual", idsDescartados.size());
        model.addAttribute("intervaloSeleccionado", intervalo);
        // Bandera para que la vista recuerde que estábamos en modo manual
        model.addAttribute("modoManual", true);

        return "sorteo/admin-sorteo-panel";
    }

    /**
     * Endpoint ajustado: Corregido el mapeo de URL para evitar duplicidad conflictiva con el RequestMapping principal.
     * Valida el umbral mínimo antes de permitir finalizar la rifa.
     */
    @PostMapping("/{id}/finalizar")
    public String finalizarRifa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Solicitud recibida para finalizar y validar sorteo de la rifa ID: {}", id);

        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Intento de finalizar rifa inexistente ID: {}", id);
                    return new RuntimeException("Rifa no encontrada");
                });

        // Validación estricta del umbral mínimo de ventas inyectado en servicio[cite: 3]
        if (!sorteoService.cumpleUmbralMinimo(id)) {
            log.warn("Bloqueo de finalización: La rifa ID {} no cumple con el porcentaje mínimo de ventas ({}%)", 
                    id, rifa.getPorcentajeMinimoVentas());
            
            redirectAttributes.addFlashAttribute("mensajeError",
                    "No se puede sortear la rifa. No se ha alcanzado el porcentaje mínimo de ventas requerido ("
                            + rifa.getPorcentajeMinimoVentas() + "%). Debe utilizar la opción de re-agendar fecha.");
            return "redirect:/admin/sorteos/" + id;
        }

        // Cambio de estado seguro a FINALIZADA
        rifa.setEstado("FINALIZADA");
        rifaRepository.save(rifa);

        log.info("Rifa ID {} finalizada exitosamente tras cumplir validaciones normativas.", id);
        redirectAttributes.addFlashAttribute("mensajeExito", "La rifa ha sido finalizada con éxito.");
        return "redirect:/admin/sorteos";
    }
}