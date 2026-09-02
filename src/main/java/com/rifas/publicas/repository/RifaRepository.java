package com.rifas.publicas.repository;
import com.rifas.publicas.model.Rifa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
public interface RifaRepository extends JpaRepository<Rifa, Long> {
    // Filtra únicamente las rifas que estén activas para mostrarlas en las vistas públicas
    List<Rifa> findByEstado(String estado);
    // Filtra las rifas por múltiples estados
    List<Rifa> findByEstadoIn(List<String> estados);
}
