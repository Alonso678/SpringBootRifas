package com.rifas.publicas.controller;

import com.rifas.publicas.model.SugerenciaRifa;
import com.rifas.publicas.repository.SugerenciaRifaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sugerencias")
public class SugerenciaRifaController {

    private final SugerenciaRifaRepository repository;

    // Inyección por constructor: Spring se encarga de poner el repositorio aquí
    public SugerenciaRifaController(SugerenciaRifaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<?> guardarSugerencia(@RequestBody SugerenciaRifa sugerencia) {
        try {
            repository.save(sugerencia);
            return ResponseEntity.ok().body("Sugerencia guardada exitosamente");
        } catch (Exception e) {
            e.printStackTrace(); // Esto te ayudará a ver el error real en la terminal
            return ResponseEntity.internalServerError().body("Error al registrar la sugerencia");
        }
    }
}
