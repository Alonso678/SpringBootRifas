package com.rifas.publicas.sorteo.controller;

import com.rifas.publicas.model.Boleto;
import com.rifas.publicas.model.Rifa;
import com.rifas.publicas.repository.BoletoRepository;
import com.rifas.publicas.repository.RifaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/admin/sorteos")
public class AdminSorteoController {

    private final RifaRepository rifaRepository;
    private final BoletoRepository boletoRepository;

    AdminSorteoController(RifaRepository rifaRepository, BoletoRepository boletoRepository) {
        this.rifaRepository = rifaRepository;
        this.boletoRepository = boletoRepository;
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
}